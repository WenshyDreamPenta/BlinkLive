package com.blink.live.blinkstreamlib.encoder;

import java.io.IOException;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/19
 *     desc   : MediaCodec Encoder
 * </pre>
 */
public class MediaVideoEncoder extends MediaEncoder{
    public MediaVideoEncoder(MediaMuxerWrapper muxer, MediaEncoderListener mediaEncoderListener) {
        super(muxer, mediaEncoderListener);
        //todo:
    }

    @Override
    public void run() {

    }

    @Override
    public void prepare() throws IOException {

    }

    @Override
    void startRecording() {
        super.startRecording();
    }

    @Override
    void stopRecording() {
        super.stopRecording();
    }
}
