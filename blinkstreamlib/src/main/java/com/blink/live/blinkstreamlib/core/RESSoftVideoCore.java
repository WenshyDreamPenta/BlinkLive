package com.blink.live.blinkstreamlib.core;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.blink.live.blinkstreamlib.core.listeners.RESScreenShotListener;
import com.blink.live.blinkstreamlib.core.listeners.RESVideoChangeListener;
import com.blink.live.blinkstreamlib.encoder.MediaVideoEncoder;
import com.blink.live.blinkstreamlib.filter.BaseSoftVideoFilter;
import com.blink.live.blinkstreamlib.model.RESConfig;
import com.blink.live.blinkstreamlib.model.RESCoreParameters;
import com.blink.live.blinkstreamlib.rtmp.RESFlvDataCollecter;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/20
 *     desc   :
 * </pre>
 */
public class RESSoftVideoCore implements RESVideoCore{
    private RESCoreParameters resCoreParameters;
    private final Object syncOp = new Object();
    private SurfaceTexture cameraTexture;

    private int currentCamera;
    private MediaCodec videoEncoder;

    private Lock lockVideoFilter = null;
    private BaseSoftVideoFilter videoFilter;
    private VideoFilterHandler videoFilterHandler;

    public RESSoftVideoCore(RESCoreParameters resCoreParameters) {
        this.resCoreParameters = resCoreParameters;
        lockVideoFilter = new ReentrantLock(false);
        videoFilter = null;
    }

    @Override
    public void setCurrentCamera(int camIndex){
        if(currentCamera != camIndex){
            synchronized (syncOp){

            }
        }
    }

    @Override
    public boolean prepare(RESConfig resConfig) {
        //todo:
        return false;
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
        return false;
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
        public VideoFilterHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
        }
    }
}
