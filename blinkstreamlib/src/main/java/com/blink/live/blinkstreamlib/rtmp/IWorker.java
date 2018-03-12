package com.blink.live.blinkstreamlib.rtmp;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/12
 *     desc   : res推流方法接口
 * </pre>
 */
public interface IWorker {
    String getServerIpAddr();
    float getSendFrameRate();
    float getSendBufferFreePercent();
    void start(String rtmpAddr);
    void stop();
    void feed(RESFlvData flvData, int type);
    int getTotalSpeed();

}
