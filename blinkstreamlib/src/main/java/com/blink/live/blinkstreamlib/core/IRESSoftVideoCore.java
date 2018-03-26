package com.blink.live.blinkstreamlib.core;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/26
 *     desc   : 软编码接口
 * </pre>
 */
public interface IRESSoftVideoCore {

    void queueVideo(byte[] rawVideoFrame);
    void acceptVideo(byte[] src, byte[] dst);
}
