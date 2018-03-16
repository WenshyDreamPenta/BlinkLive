package com.blink.live.blinkstreamlib.core;

import android.media.MediaCodec;

import com.blink.live.blinkstreamlib.rtmp.RESFlvDataCollecter;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/15
 *     desc   : audio 发送线程
 * </pre>
 */
public class AudioSenderThread extends Thread {
    //ms
    private static final long WAIT_TIME = 5000;
    private MediaCodec.BufferInfo mBufferInfo;
    private long startTime = 0;
    private MediaCodec audioEncoder;
    private RESFlvDataCollecter dataCollecter;

    private boolean shouldQuit = false;

    AudioSenderThread(String name, MediaCodec encoder, RESFlvDataCollecter flvDataCollecter){
        super(name);
        mBufferInfo = new MediaCodec.BufferInfo();
        startTime = 0;
        audioEncoder = encoder;
        dataCollecter = flvDataCollecter;
    }

    //中断线程
    void quit(){
        shouldQuit = true;
        this.interrupt();
    }
}
