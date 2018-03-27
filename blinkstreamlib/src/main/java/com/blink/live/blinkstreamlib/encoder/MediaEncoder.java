package com.blink.live.blinkstreamlib.encoder;

import android.media.MediaCodec;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;

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
    /**
     * Flag that indicate this encoder is capturing now.
     */
    protected volatile boolean mIsCapturing;
    /**
     * Flag that indicate the frame data will be available soon.
     */
    private int mRequestDrain;
    /**
     * Flag to request stop capturing
     */
    protected volatile boolean mRequestStop;
    /**
     * Flag that indicate encoder received EOS(End Of Stream)
     */
    protected boolean mIsEOS;
    /**
     * Flag the indicate the muxer is running
     */
    protected boolean mMuxerStarted;
    /**
     * Track Number
     */
    protected int mTrackIndex;
    /**
     * MediaCodec instance for encoding
     */
    protected MediaCodec mMediaCodec;                // API >= 16(Android4.1.2)
    /**
     * Weak refarence of MediaMuxerWarapper instance
     */
    protected final WeakReference<MediaMuxerWrapper> mWeakMuxer;
    /**
     * BufferInfo instance for dequeuing
     */
    private MediaCodec.BufferInfo mBufferInfo;        // API >= 16(Android4.1.2)


    protected final MediaEncoderListener mediaEncoderListener;

    public MediaEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener mediaEncoderListener) {
        if (mediaEncoderListener == null) {
            throw new NullPointerException("MediaEncoderListener is null");
        }
        if (muxer == null) {
            throw new NullPointerException("MediaMuxerWrapper is null");
        }
        this.mediaEncoderListener = mediaEncoderListener;
        this.mWeakMuxer = new WeakReference<>(muxer);
        muxer.addEncoder(this);
        synchronized (SyncO) {
            mBufferInfo = new MediaCodec.BufferInfo();
            new Thread(this, getClass().getSimpleName()).start();
            try {
                SyncO.wait();
            }
            catch (InterruptedException e) {
                throw new NullPointerException("Thread InterruptedException");
            }
        }
    }

    void startRecording() {
        if (DEBUG) {
            Log.v(TAG, "startRecording");
        }
        synchronized (SyncO) {
            mIsCapturing = true;
            mRequestStop = false;
            SyncO.notifyAll();
        }
    }

    void stopRecording() {
        if (DEBUG)
            Log.v(TAG, "stopRecording");
        synchronized (SyncO) {
            if (!mIsCapturing || mRequestStop) {
                return;
            }
            mRequestStop = true;    // for rejecting newer frame
            SyncO.notifyAll();
        }
    }

    public void release(){
        //todo:
    }

    public abstract void prepare() throws IOException;

    public interface MediaEncoderListener {
        void onPrepared(MediaEncoder encoder);

        void onStopped(MediaEncoder encoder);
    }
}
