package com.blink.live.blinkstreamlib.core;

import java.util.LinkedList;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/12
 *     desc   : audio byte数据
 * </pre>
 */
public class StreamByteSpeedometer {
    private int timeGranularity;
    private LinkedList<ByteFrame> byteList;
    private final Object syncByteList = new Object();

    public StreamByteSpeedometer(int timeGranularity) {
        this.timeGranularity = timeGranularity;
        byteList = new LinkedList<>();
    }

    public int getSpeed() {
        synchronized (syncByteList) {
            long now = System.currentTimeMillis();
            trim(now);
            long sumByte = 0;
            for (ByteFrame byteFrame : byteList) {
                sumByte += byteFrame.bytenum;
            }
            return (int) (sumByte * 1000 / timeGranularity);
        }
    }

    public void gain(int byteCount) {
        synchronized (syncByteList) {
            long now = System.currentTimeMillis();
            byteList.addLast(new ByteFrame(now, byteCount));
            trim(now);
        }
    }

    private void trim(long time) {
        while (!byteList.isEmpty() && (time - byteList.getFirst().time) > timeGranularity) {
            byteList.removeFirst();
        }
    }

    public void reset() {
        synchronized (syncByteList) {
            byteList.clear();
        }
    }

    private class ByteFrame {
        long time;
        long bytenum;

        public ByteFrame(long time, long bytenum) {
            this.time = time;
            this.bytenum = bytenum;
        }
    }
}
