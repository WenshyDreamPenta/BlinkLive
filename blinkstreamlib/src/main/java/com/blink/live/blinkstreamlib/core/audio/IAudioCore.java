package com.blink.live.blinkstreamlib.core.audio;

import com.blink.live.blinkstreamlib.model.StreamConfig;
import com.blink.live.blinkstreamlib.rtmp.StreamFlvDataCollecter;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/4/12
 *     desc   : Audio接口
 * </pre>
 */
public interface IAudioCore {
    /**
     * audio数据入口
     * @param rawAudioFrame 音频byte数组
     */
    void queueAudio(byte[] rawAudioFrame);

    /**
     * prepare
     * @param streamConfig 配置
     * @return boolean
     */
    boolean prepare(StreamConfig streamConfig);

    /**
     * 开启编码
     * @param streamFlvDataCollecter 数据收集器
     */
    void start(StreamFlvDataCollecter streamFlvDataCollecter);

    /**
     * 停止编码
     */
    void stop();

    /**
     * 销毁
     */
    void destroy();
}
