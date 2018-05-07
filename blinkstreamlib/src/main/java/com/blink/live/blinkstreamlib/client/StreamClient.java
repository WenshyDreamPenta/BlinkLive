package com.blink.live.blinkstreamlib.client;

import android.app.Activity;
import android.content.Context;

import com.blink.live.blinkstreamlib.model.StreamConfig;
import com.blink.live.blinkstreamlib.model.StreamCoreParameters;
import com.blink.live.blinkstreamlib.rtmp.RtmpPusher;
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

    public void setContext(Context context){
        if(context instanceof Activity){
            this.mActivity = new WeakReference<>((Activity) context);
        }
    }

    public boolean prepare(StreamConfig streamConfig){
        synchronized (SyncOp){
            
        }
        return false;
    }
}
