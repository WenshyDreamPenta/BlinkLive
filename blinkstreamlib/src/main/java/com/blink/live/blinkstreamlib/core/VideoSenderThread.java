package com.blink.live.blinkstreamlib.core;

import android.media.MediaCodec;

import com.blink.live.blinkstreamlib.rtmp.RESFlvDataCollecter;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/13
 *     desc   : vedio 发送线程
 * </pre>
 */
public class VideoSenderThread extends Thread{
    private static final long WAIT_TIME = 5000;
    private MediaCodec.BufferInfo eInfo;
    private long startTime = 0;
    private MediaCodec dstVideoEncoder;
    private final Object syncDstVideoEncoder = new Object();
    private RESFlvDataCollecter dataCollecter;
    private boolean shouldQuit = false;

    VideoSenderThread(String name, MediaCodec encoder, RESFlvDataCollecter flvDataCollecter) {
        super(name);
        eInfo = new MediaCodec.BufferInfo();
        startTime = 0;
        dstVideoEncoder = encoder;
        dataCollecter = flvDataCollecter;
    }

    //更新编码器
    public void updateMediaCodec(MediaCodec encodec){
        synchronized (syncDstVideoEncoder){
            dstVideoEncoder =encodec;
        }
    }

    //退出线程
    void quit(){
        shouldQuit = true;
        this.interrupt();
    }

    @Override
    public void run() {

    }
}
