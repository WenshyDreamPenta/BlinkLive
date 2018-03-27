package com.blink.live.blinkstreamlib.encoder;

import android.media.MediaCodec;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/27
 *     desc   : Ecoder abstract class
 * </pre>
 */
public abstract class MediaEncoder implements Runnable {
    private static final boolean DEBUG = false;
    private static final String TAG = "MediaEncoder";
    protected static final int TIMEOUT_USEC = 10000;
    protected static final int MSG_FRAME_AVAILABLE = 1;
    protected static final int MSG_STOP_RECORDING = 9;
    protected final Object SyncO = new Object();

    public interface MediaEncoderListener {
        void onPrepared(MediaEncoder encoder);

        void onStopped(MediaEncoder encoder);
    }

    private MediaCodec.BufferInfo mBufferInfo;        // API >= 16(Android4.1.2)
    protected MediaCodec mMediaCodec;                // API >= 16(Android4.1.2)
    protected final MediaEncoderListener mediaEncoderListener;

    public MediaEncoder(MediaEncoderListener mediaEncoderListener) {
        this.mediaEncoderListener = mediaEncoderListener;
    }



}
