package com.blink.live.blinkstreamlib.client;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.widget.Toast;

import com.blink.live.blinkstreamlib.model.StreamConfig;
import com.blink.live.blinkstreamlib.model.StreamCoreParameters;
import com.blink.live.blinkstreamlib.rtmp.RtmpPusher;
import com.blink.live.blinkstreamlib.rtmp.StreamFlvData;
import com.blink.live.blinkstreamlib.rtmp.StreamFlvDataCollecter;
import com.blink.live.blinkstreamlib.tools.CallbackDelivery;

import java.lang.ref.WeakReference;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/4/12
 *     desc   : Stream Client Proxy
 * </pre>
 */
public class StreamClient {
    private StreamAudioClient streamAudioClient;
    private StreamVideoClient streamVideoClient;
    private final Object SyncOp;

    //parameters
    private StreamCoreParameters coreParameters;
    private RtmpPusher rtmpPusher;
    private StreamFlvDataCollecter dataCollecter;

    //判断是否推流
    private boolean isStreaming = false;
    private WeakReference<Activity> mActivity;

    public StreamClient() {
        SyncOp = new Object();
        coreParameters = new StreamCoreParameters();
        CallbackDelivery.initInstance();
    }

    //设置弱引用Context
    public void setContext(Context context) {
        if (context instanceof Activity) {
            this.mActivity = new WeakReference<>((Activity) context);
        }
    }

    //初始化
    public boolean prepare(StreamConfig streamConfig) {
        synchronized (SyncOp) {
            checkDirection(streamConfig);
            coreParameters.filterMode = streamConfig.getFilterMode();
            coreParameters.rtmpAddr = streamConfig.getRtmpAddr();
            coreParameters.printDetailMsg = streamConfig.isPrintDetailMsg();
            coreParameters.senderQueueLength = 200;//150
            streamVideoClient = new StreamVideoClient(coreParameters);
            streamAudioClient = new StreamAudioClient(coreParameters);
            if (!streamVideoClient.prepare(streamConfig) ||
                    !streamAudioClient.prepare(streamConfig)) {
                return false;
            }
            rtmpPusher = new RtmpPusher();
            rtmpPusher.preapare(coreParameters);
            dataCollecter = new StreamFlvDataCollecter() {
                @Override
                public void collect(StreamFlvData flvData, int type) {
                    if (rtmpPusher != null) {
                        rtmpPusher.feed(flvData, type);
                    }
                }
            };
            coreParameters.done = true;
            return true;
        }
    }

    //开始推流
    public void startStreaming(String rtmp) {
        isStreaming = true;
        synchronized (SyncOp) {
            try {
                streamVideoClient.startStreaming(dataCollecter);
                rtmpPusher.start(rtmp == null ? coreParameters.rtmpAddr : rtmp);
                streamAudioClient.start(dataCollecter);
            }
            catch (Exception e) {
                if (mActivity.get() != null) {
                    Toast.makeText(mActivity.get(), "可能没有权限", Toast.LENGTH_LONG).show();
                    mActivity.get().finish();
                }
            }
        }
    }

    //开始推流2
    public void startStreaming() {
        isStreaming = true;
        synchronized (SyncOp) {
            streamVideoClient.startStreaming(dataCollecter);
            rtmpPusher.start(coreParameters.rtmpAddr);
            streamAudioClient.start(dataCollecter);
        }
    }

    //停止推流
    public void stopStreaming() {
        isStreaming = false;
        synchronized (SyncOp) {
            streamVideoClient.stopStreaming();
            streamAudioClient.stop();
            rtmpPusher.stop();
        }
    }

    //销毁
    public void destroy() {
        synchronized (SyncOp) {
            rtmpPusher.destroy();
            streamAudioClient.destroy();
            streamVideoClient.destroy();
            rtmpPusher = null;
            streamVideoClient = null;
            streamAudioClient = null;
        }
    }

    //开始相机预览
    public void startPreview(SurfaceTexture surfaceTexture, int visualWidth, int visualHeight) {
        if (streamVideoClient != null) {
            streamVideoClient.startPreview(surfaceTexture, visualWidth, visualHeight);
        }
    }

    //更新预览
    public void updatePreview(int visualwidth, int visualHeight) {
        if (streamVideoClient != null) {
            streamVideoClient.updatePreview(visualwidth, visualHeight);
        }
    }

    //停止预览
    public void stopPreview(boolean releaseTexture) {
        if (streamVideoClient != null) {
            streamVideoClient.stopPreview(releaseTexture);
        }
    }

    //切换摄像头
    public boolean swtichCamera(){
        synchronized (SyncOp){
            return streamVideoClient.swtichCamera();
        }
    }

    //检查方向
    private void checkDirection(StreamConfig streamConfig) {
        int frontFlag = streamConfig.getFrontCameraDirectionMode();
        int backFlag = streamConfig.getBackCameraDirectionMode();
        int fbit = 0;
        int bbit = 0;
        if ((frontFlag >> 4) == 0) {
            frontFlag |= StreamCoreParameters.FLAG_DIRECTION_ROATATION_0;
        }
        if ((backFlag >> 4) == 0) {
            backFlag |= StreamCoreParameters.FLAG_DIRECTION_ROATATION_0;
        }
        for (int i = 4; i <= 8; ++i) {
            if (((frontFlag >> i) & 0x1) == 1) {
                fbit++;
            }
            if (((backFlag >> i) & 0x1) == 1) {
                bbit++;
            }
        }
        if (fbit != 1 || bbit != 1) {
            throw new RuntimeException(
                    "invalid direction rotation flag:frontFlagNum=" + fbit + ",backFlagNum=" +
                            bbit);
        }
        if (((frontFlag & StreamCoreParameters.FLAG_DIRECTION_ROATATION_0) != 0) ||
                ((frontFlag & StreamCoreParameters.FLAG_DIRECTION_ROATATION_180) != 0)) {
            fbit = 0;
        }
        else {
            fbit = 1;
        }
        if (((backFlag & StreamCoreParameters.FLAG_DIRECTION_ROATATION_0) != 0) ||
                ((backFlag & StreamCoreParameters.FLAG_DIRECTION_ROATATION_180) != 0)) {
            bbit = 0;
        }
        else {
            bbit = 1;
        }
        if (bbit != fbit) {
            if (bbit == 0) {
                throw new RuntimeException("invalid direction rotation flag:back camera is landscape but front camera is portrait");
            }
            else {
                throw new RuntimeException("invalid direction rotation flag:back camera is portrait but front camera is landscape");
            }
        }
        if (fbit == 1) {
            coreParameters.isPortrait = true;
        }
        else {
            coreParameters.isPortrait = false;
        }
        coreParameters.backCameraDirectionMode = backFlag;
        coreParameters.frontCameraDirectionMode = frontFlag;
    }

}
