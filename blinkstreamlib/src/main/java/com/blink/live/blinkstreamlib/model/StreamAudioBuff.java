package com.blink.live.blinkstreamlib.model;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/4/12
 *     desc   : AudioBuff 类
 * </pre>
 */
public class StreamAudioBuff {
    public boolean isReadyToFill;
    public int audioFormat = -1;
    public byte[] buff;

    public StreamAudioBuff(int audioFormat, int size) {
        isReadyToFill = true;
        this.audioFormat = audioFormat;
        buff = new byte[size];
    }
}
