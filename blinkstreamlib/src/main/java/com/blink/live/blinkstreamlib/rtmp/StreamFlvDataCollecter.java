package com.blink.live.blinkstreamlib.rtmp;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/12
 *     desc   : RESFlv数据 收集接口
 * </pre>
 */
public interface StreamFlvDataCollecter {
    void collect(StreamFlvData flvData, int type);
}
