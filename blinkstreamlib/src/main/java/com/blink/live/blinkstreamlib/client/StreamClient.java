package com.blink.live.blinkstreamlib.client;

import android.app.Activity;
import android.content.Context;

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
 *     desc   : Stream Client
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

    public StreamClient(){
        SyncOp = new Object();
        coreParameters = new StreamCoreParameters();
        CallbackDelivery.initInstance();
    }

    //设置弱引用Context
    public void setContext(Context context){
        if(context instanceof Activity){
            this.mActivity = new WeakReference<>((Activity) context);
        }
    }

    //初始化
    public boolean prepare(StreamConfig streamConfig){
        synchronized (SyncOp){
            checkDirection(streamConfig);
            coreParameters.filterMode = streamConfig.getFilterMode();
            coreParameters.rtmpAddr = streamConfig.getRtmpAddr();
            coreParameters.printDetailMsg = streamConfig.isPrintDetailMsg();
            coreParameters.senderQueueLength = 200;//150
            streamVideoClient = new StreamVideoClient(coreParameters);
            streamAudioClient = new StreamAudioClient(coreParameters);
            if(!streamVideoClient.prepare(streamConfig) || !streamAudioClient.prepare(streamConfig)){
                return false;
            }
            rtmpPusher = new RtmpPusher();
            rtmpPusher.preapare(coreParameters);
            dataCollecter = new StreamFlvDataCollecter() {
                @Override
                public void collect(StreamFlvData flvData, int type) {
                    if(rtmpPusher != null){
                        rtmpPusher.feed(flvData, type);
                    }
                }
            };
            coreParameters.done = true;
            return true;
        }
    }

    private void checkDirection(StreamConfig streamConfig){
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
