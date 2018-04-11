package com.blink.live.blinkstreamlib.model;

import java.util.Arrays;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/23
 *     desc   : VideoBuff 类
 * </pre>
 */
public class StreamVideoBuff {
    public boolean isReadyToFill;
    public int colorFormat = -1;
    public byte[] buff;


    public StreamVideoBuff(int colorFormat, int size) {
        isReadyToFill = true;
        this.colorFormat = colorFormat;
        buff = new byte[size];
        Arrays.fill(buff, size / 2, size, (byte) 127);
    }
}
