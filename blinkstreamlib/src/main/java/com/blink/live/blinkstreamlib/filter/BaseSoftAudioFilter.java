package com.blink.live.blinkstreamlib.filter;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/4/12
 *     desc   : audio filter
 * </pre>
 */
public class BaseSoftAudioFilter {
    protected int SIZE;
    protected int SIZE_HALF;

    public void onInit(int size) {
        SIZE = size;
        SIZE_HALF = size / 2;
    }

    /**
     * @param orignBuff 原始数据
     * @param targetBuff 目标数据
     * @param presentationTimeMs 展示时间
     * @param sequenceNum 序列数
     * @return false to use orignBuff,true to use targetBuff
     */
    public boolean onFrame(byte[] orignBuff, byte[] targetBuff, long presentationTimeMs, int sequenceNum) {
        return false;
    }

    public void onDestroy() {

    }
}
