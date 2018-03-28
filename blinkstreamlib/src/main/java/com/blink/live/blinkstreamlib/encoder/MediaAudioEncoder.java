package com.blink.live.blinkstreamlib.encoder;

import java.io.IOException;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/27
 *     desc   :
 * </pre>
 */
public class MediaAudioEncoder  extends MediaEncoder{
    private static final boolean DEBUG = false;
    private static final String TAG = "MediaAudioEncoder";
    public MediaAudioEncoder(MediaMuxerWrapper muxer, MediaEncoderListener mediaEncoderListener) {
        super(muxer, mediaEncoderListener);
    }

    @Override
    public void prepare() throws IOException {

    }

    @Override
    void startRecording() {
    }

    @Override
    void stopRecording() {
    }

    @Override
    public void run() {

    }
}
