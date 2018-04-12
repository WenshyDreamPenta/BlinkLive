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
    void queueAudio(byte[] rawAudioFrame);
    void prepare(StreamConfig streamConfig);
    void start(StreamFlvDataCollecter streamFlvDataCollecter);
    void stop();
    void destroy();
}
