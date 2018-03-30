package com.blink.live.blinkstreamlib.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

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
    /**
     * previous presentationTimeUs for writing
     */
    private long prevOutputPTSUs = 0;


    protected final MediaEncoderListener mediaEncoderListener;

    public MediaEncoder(final MediaMuxerWrapper muxer,
            final MediaEncoderListener mediaEncoderListener) {
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

    @Override
    public void run() {
        synchronized (SyncO) {
            mRequestStop = false;
            mRequestDrain = 0;
            SyncO.notifyAll();
        }
        final boolean isRunning = true;
        boolean localRequestStop;
        boolean localRequestDrain;
        while (isRunning) {
            synchronized (SyncO) {
                localRequestDrain = mRequestDrain > 0;
                localRequestStop = mRequestStop;
                if (localRequestDrain) {
                    mRequestDrain--;
                }
                if (localRequestStop) {
                    drain();
                    signalEndOfInputStream();
                    drain();
                    release();
                    break;
                }
                if (localRequestDrain) {
                    drain();
                }
                else {
                    synchronized (SyncO) {
                        try {
                            SyncO.wait();
                        }
                        catch (Exception e) {
                            break;
                        }
                    }

                }
            }
        }
        if (DEBUG) {
            Log.d(TAG, "Encoder thread exiting");
        }
        synchronized (SyncO) {
            mIsCapturing = false;
            mRequestStop = true;
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
        if (DEBUG) {
            Log.v(TAG, "stopRecording");
        }
        synchronized (SyncO) {
            if (!mIsCapturing || mRequestStop) {
                return;
            }
            mRequestStop = true;    // for rejecting newer frame
            SyncO.notifyAll();
        }
    }

    protected void signalEndOfInputStream() {
        if (DEBUG)
            Log.d(TAG, "sending EOS to encoder");
        // signalEndOfInputStream is only avairable for video encoding with surface
        // and equivalent sending a empty buffer with BUFFER_FLAG_END_OF_STREAM flag.
        //		mMediaCodec.signalEndOfInputStream();	// API >= 18
        encode(null, 0, getPTSUs());
    }

    public void release() {
        if (DEBUG) {
            Log.d(TAG, "release: ");
        }
        try {
            mediaEncoderListener.onStopped(this);
        }
        catch (Exception e) {
            Log.e(TAG, "failed onStopped", e);
        }
        mIsCapturing = false;
        if (mMediaCodec != null) {
            try {
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
            }
            catch (Exception e) {
                Log.e(TAG, "failed releasing MediaCodec", e);
            }
            if (mMuxerStarted) {
                final MediaMuxerWrapper muxerWrapper = mWeakMuxer != null ? mWeakMuxer.get() : null;
                if (muxerWrapper != null) {
                    try {
                        muxerWrapper.stop();
                    }
                    catch (Exception e) {
                        Log.e(TAG, "failed stopping muxer", e);
                    }
                }
            }
            mBufferInfo = null;
        }
    }

    /**
     * Method to set byte array to the MediaCodec encoder
     *
     * @param buffer             input byte array
     * @param length             length of byte array, zero means EOS.
     * @param presentationTimeUs
     */
    protected void encode(final ByteBuffer buffer, final int length,
            final long presentationTimeUs) {
        if (!mIsCapturing) {
            return;
        }
        final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        while (mIsCapturing) {
            final int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
            Log.d(TAG, "inputBufferIndex: " + inputBufferIndex);
            if (inputBufferIndex >= 0) {
                final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                if (buffer != null) {
                    inputBuffer.put(buffer);
                }
                if (DEBUG) {
                    Log.v(TAG, "encode:queueInputBuffer");
                }
                if (length <= 0) {
                    mIsEOS = true;
                    if (DEBUG) {
                        Log.i(TAG, "send BUFFER_FLAG_END_OF_STREAM");
                    }
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    break;
                }
                else {
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length, presentationTimeUs, 0);
                }
                break;
            }
        }
    }

    protected void drain() {
        if (mMediaCodec == null) {
            return;
        }
        ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();
        Log.e(TAG, "encoderOutputBuffers: " + encoderOutputBuffers.length);
        int encoderStatus;
        int count = 0;
        final MediaMuxerWrapper muxerWrapper = mWeakMuxer.get();
        if (muxerWrapper == null) {
            Log.w(TAG, "muxer is unexpectedly null");
            return;
        }
        LOOP:
        while (mIsCapturing) {
            encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!mIsEOS) {
                    if (++count > 5) {
                        break LOOP;
                    }
                }
            }
            else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                encoderOutputBuffers = mMediaCodec.getOutputBuffers();
            }
            else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                final MediaFormat format = mMediaCodec.getOutputFormat();
                mTrackIndex = muxerWrapper.addTrack(format);
                mMuxerStarted = true;
                if (!muxerWrapper.start()) {
                    synchronized (muxerWrapper) {
                        while (!muxerWrapper.isStarted()) {
                            try {
                                muxerWrapper.wait(100);
                            }
                            catch (Exception e) {
                                break LOOP;
                            }
                        }
                    }
                }
            }
            else if (encoderStatus < 0) {
                if (DEBUG)
                    Log.w(TAG, "drain:unexpected result from encoder#dequeueOutputBuffer: " +
                            encoderStatus);
            }
            else {
                final ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException(
                            "encoderOutputBuffer " + encoderStatus + " was null");
                }
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mBufferInfo.size = 0;
                }
                if (mBufferInfo.size != 0) {
                    count = 0;
                    if (!mMuxerStarted) {
                        throw new RuntimeException("drain:muxer hasn't started");
                    }
                    mBufferInfo.presentationTimeUs = getPTSUs();
                    muxerWrapper.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                    prevOutputPTSUs = mBufferInfo.presentationTimeUs;
                }
                mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    mIsCapturing = false;
                    break;
                }
            }

        }
    }

    /**
     * get next encoding presentationTimeUs
     *
     * @return
     */
    protected long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        if (result < prevOutputPTSUs)
            result = (prevOutputPTSUs - result) + result;
        return result;
    }

    public abstract void prepare() throws IOException;

    public interface MediaEncoderListener {
        void onPrepared(MediaEncoder encoder);

        void onStopped(MediaEncoder encoder);
    }
}
