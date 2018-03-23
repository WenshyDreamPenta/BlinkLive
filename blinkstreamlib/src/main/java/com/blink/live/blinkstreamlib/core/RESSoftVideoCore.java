package com.blink.live.blinkstreamlib.core;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import com.blink.live.blinkstreamlib.core.listeners.RESScreenShotListener;
import com.blink.live.blinkstreamlib.core.listeners.RESVideoChangeListener;
import com.blink.live.blinkstreamlib.encoder.MediaVideoEncoder;
import com.blink.live.blinkstreamlib.filter.BaseSoftVideoFilter;
import com.blink.live.blinkstreamlib.model.RESConfig;
import com.blink.live.blinkstreamlib.model.RESCoreParameters;
import com.blink.live.blinkstreamlib.model.RESVideoBuff;
import com.blink.live.blinkstreamlib.rtmp.RESFlvDataCollecter;
import com.blink.live.blinkstreamlib.utils.BuffSizeCalculator;
import com.blink.live.blinkstreamlib.utils.LogTools;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/20
 *     desc   : 视频软编核心类
 * </pre>
 */
public class RESSoftVideoCore implements RESVideoCore {
    private RESCoreParameters resCoreParameters;
    private final Object syncOp = new Object();
    private SurfaceTexture cameraTexture;

    private int currentCamera;
    private MediaCodec videoEncoder;

    private final Object syncDstVideoEncoder = new Object();
    private MediaFormat dstVideoFormat;

    private Lock lockVideoFilter = null;
    private BaseSoftVideoFilter videoFilter;
    private VideoFilterHandler videoFilterHandler;
    private HandlerThread videoFilterHandlerThread;
    //sender
    private VideoSenderThread videoSenderThread;
    //VideoBuffs
    //buffers to handle buff from queueVideo
    private RESVideoBuff[] orignVideoBuffs;
    private int lastVideoQueueBuffIndex;
    //buffer to convert orignVideoBuff to NV21 if filter are set
    private RESVideoBuff orignNV21VideoBuff;
    //buffer to handle filtered color from filter if filter are set
    private RESVideoBuff filteredNV21VideoBuff;
    //buffer to convert other color format to suitable color format for dstVideoEncoder if nessesary
    private RESVideoBuff suitable4VideoEncoderBuff;

    private final Object syncIsLooping = new Object();
    private boolean isPreviewing = false;
    private boolean isStreaming = false;
    private boolean isEncoderStarted;
    private int loopingInterval;

    public RESSoftVideoCore(RESCoreParameters resCoreParameters) {
        this.resCoreParameters = resCoreParameters;
        lockVideoFilter = new ReentrantLock(false);//重入锁
        videoFilter = null;
    }

    @Override
    public void setCurrentCamera(int camIndex) {
        if (currentCamera != camIndex) {
            synchronized (syncOp) {
                if (videoFilterHandler != null) {
                    videoFilterHandler.removeMessages(VideoFilterHandler.WHAT_INCOMING_BUFF);
                }
                if (orignVideoBuffs != null) {
                    for (RESVideoBuff buff : orignVideoBuffs) {
                        buff.isReadyToFill = true;
                    }
                    lastVideoQueueBuffIndex = 0;
                }

            }
        }
        currentCamera = camIndex;
    }

    @Override
    public boolean prepare(RESConfig resConfig) {
        synchronized (syncOp) {
            resCoreParameters.renderingMode = resConfig.getRenderingMode();
            resCoreParameters.mediacdoecAVCBitRate = resConfig.getBitRate();
            resCoreParameters.videoBufferQueueNum = resConfig.getVideoBufferQueueNum();
            resCoreParameters.mediacodecAVCIFrameInterval = resConfig.getVideoGOP();
            resCoreParameters.mediacodecAVCFrameRate = resCoreParameters.videoFPS;
            loopingInterval = 1000 / resCoreParameters.videoFPS;
            dstVideoFormat = new MediaFormat();
            synchronized (syncDstVideoEncoder) {
                videoEncoder = MediaCodecHelper.createSoftVideoMediaCodec(resCoreParameters, dstVideoFormat);
                isEncoderStarted = false;
                if (videoEncoder == null) {
                    LogTools.e("create video Mediacodec failed");
                    return false;
                }
                resCoreParameters.previewBufferSize = BuffSizeCalculator.calculator(resCoreParameters.videoWidth, resCoreParameters.videoHeight, resCoreParameters.previewColorFormat);
                //video
                int videoWidth = resCoreParameters.videoWidth;
                int videoHeight = resCoreParameters.videoHeight;
                int videoQueueNum = resCoreParameters.videoBufferQueueNum;
                orignVideoBuffs = new RESVideoBuff[videoQueueNum];
                for (int i = 0; i < videoQueueNum; i++) {
                    orignVideoBuffs[i] = new RESVideoBuff(resCoreParameters.previewColorFormat, resCoreParameters.previewBufferSize);
                }
                lastVideoQueueBuffIndex = 0;
                orignNV21VideoBuff = new RESVideoBuff(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar, BuffSizeCalculator
                        .calculator(videoWidth, videoHeight, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar));
                filteredNV21VideoBuff = new RESVideoBuff(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar, BuffSizeCalculator
                        .calculator(videoWidth, videoHeight, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar));
                suitable4VideoEncoderBuff = new RESVideoBuff(resCoreParameters.mediacodecAVCColorFormat, BuffSizeCalculator
                        .calculator(videoWidth, videoHeight, resCoreParameters.mediacodecAVCColorFormat));
                videoFilterHandlerThread = new HandlerThread("videoFilterHandlerThread");
                videoFilterHandlerThread.start();
                videoFilterHandler = new VideoFilterHandler(videoFilterHandlerThread.getLooper());

                return true;

            }
        }
    }

    @Override
    public void updateCamTexture(SurfaceTexture camTex) {

    }

    @Override
    public void startPreview(SurfaceTexture surfaceTexture, int visualWidth, int visualHeight) {

    }

    @Override
    public void updatePreview(int visualWidth, int visualHeight) {

    }

    @Override
    public void stopPreview(boolean releaseTexture) {

    }

    @Override
    public boolean startStreaming(RESFlvDataCollecter flvDataCollecter) {
        synchronized (syncOp){
            try {
                synchronized (syncDstVideoEncoder) {
                    if (videoEncoder == null) {
                        //create Encoder
                        videoEncoder = MediaCodec.createEncoderByType(dstVideoFormat.getString(MediaFormat.KEY_MIME));
                    }
                }
                videoEncoder.configure(dstVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                videoEncoder.start();
                isEncoderStarted = true;
                videoSenderThread = new VideoSenderThread("VideoSenderThread", videoEncoder, flvDataCollecter);
                videoSenderThread.start();
                synchronized (syncIsLooping){
                    if(!isPreviewing && !isStreaming){
                        videoFilterHandler.removeMessages(VideoFilterHandler.WHAT_DRAW);
                        videoFilterHandler.sendMessageDelayed(videoFilterHandler.obtainMessage(VideoFilterHandler.WHAT_DRAW, SystemClock
                                .uptimeMillis() + loopingInterval), loopingInterval);
                    }
                    isStreaming = true;
                }
            }
            catch(Exception e){
                LogTools.trace("RESVideoClient.start failed", e);
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean stopStreaming() {
        return false;
    }

    @Override
    public boolean destroy() {
        return false;
    }

    @Override
    public void reSetVideoBitrate(int bitrate) {

    }

    @Override
    public int getVideoBitrate() {
        return 0;
    }

    @Override
    public void reSetVideoFPS(int fps) {

    }

    @Override
    public void reSetVideoSize(RESCoreParameters newParameters) {

    }

    @Override
    public void takeScreenShot(RESScreenShotListener listener) {

    }

    @Override
    public void setVideoChangeListener(RESVideoChangeListener listener) {

    }

    @Override
    public float getDrawFrameRate() {
        return 0;
    }

    @Override
    public void setVideoEncoder(MediaVideoEncoder encoder) {

    }

    @Override
    public void setMirror(boolean isEnableMirror, boolean isEnablePreviewMirror,
            boolean isEnableStreamMirror) {

    }

    private class VideoFilterHandler extends Handler {
        public static final int FILTER_LOCK_TOLERATION = 3;//3ms
        public static final int WHAT_INCOMING_BUFF = 1;
        public static final int WHAT_DRAW = 2;
        public static final int WHAT_RESET_BITRATE = 3;

        public VideoFilterHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
        }
    }
}
