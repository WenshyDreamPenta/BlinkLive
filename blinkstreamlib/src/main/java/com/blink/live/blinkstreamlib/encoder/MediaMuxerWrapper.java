package com.blink.live.blinkstreamlib.encoder;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.blink.live.blinkstreamlib.utils.FileUtil;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/27
 *     desc   : Mediao Muxer
 * </pre>
 */
public class MediaMuxerWrapper {
    private static final boolean DEBUG = false;
    private static final String TAG = "MediaMuxerWrapper";
    private static final String DIR_NAME = "BlinkLive";

    private String mOutputPath;
    private final android.media.MediaMuxer mMediaMuxer;
    private int mEncoderCount, mStatredCount;
    private boolean mIsStarted;
    private MediaEncoder mVideoEncoder, mAudioEncoder;
    public static final String ROOT_DIR = "video";
    private static final String DIR_TMP = "tmp";
    private Context mContext;

    public MediaMuxerWrapper(String ext) throws IOException {
        if(TextUtils.isEmpty(ext)){
            ext = ".mp4";
        }
        try{
            mOutputPath = FileUtil.getCaptureFile(Environment.DIRECTORY_MOVIES, ext, DIR_NAME).toString();
        }
        catch (NullPointerException e){
            throw new RuntimeException("This app has no permission of writing external storage");
        }
        mMediaMuxer = new android.media.MediaMuxer(mOutputPath, android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        mEncoderCount = mStatredCount = 0;
        mIsStarted = false;
    }


    public void prepare() throws IOException{
        if(mVideoEncoder != null){
            mVideoEncoder.prepare();
        }
        if(mAudioEncoder != null){
            mAudioEncoder.prepare();
        }
    }

    public void startRecording() {
        if (mVideoEncoder != null)
            mVideoEncoder.startRecording();
        if (mAudioEncoder != null)
            mAudioEncoder.startRecording();
    }

    public void stopRecording() {
        if (mVideoEncoder != null)
            mVideoEncoder.stopRecording();
        mVideoEncoder = null;
        if (mAudioEncoder != null)
            mAudioEncoder.stopRecording();
        mAudioEncoder = null;
    }

    public void addEncoder(MediaEncoder encoder){
        if(encoder instanceof MediaVideoEncoder){
            if(mVideoEncoder != null){
                throw new IllegalArgumentException("Video encoder already added.");
            }
            mVideoEncoder = encoder;
        }
        else if(encoder instanceof MediaAudioEncoder){
            if(mAudioEncoder != null){
                throw new IllegalArgumentException("Audio encoder already added.");
            }
            mAudioEncoder = encoder;
        }
        else {
            throw new IllegalArgumentException("unsupported encoder");
        }
        mEncoderCount = (mVideoEncoder != null ? 1 : 0) + (mAudioEncoder != null ? 1 : 0);
    }

    public synchronized boolean start(){
        if(DEBUG){
            Log.v(TAG,  "start:");
        }
        mStatredCount ++;
        if((mEncoderCount > 0) && (mStatredCount == mEncoderCount)){
            mMediaMuxer.start();
            mIsStarted = true;
            notifyAll();
            if (DEBUG){
                Log.v(TAG,  "MediaMuxer started:");
            }
        }
        return mIsStarted;
    }

    /**
     * request stop recording from encoder when encoder received EOS
     */
	public synchronized void stop() {
        if (DEBUG) {
            Log.v(TAG,  "stop:mStatredCount=" + mStatredCount);
        }
        mStatredCount --;
        if ((mEncoderCount > 0) && (mStatredCount <= 0)) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mIsStarted = false;
            if (DEBUG) {
                Log.v(TAG,  "MediaMuxer stopped:");
            }
        }
    }

    /**
     * assign encoder to muxer
     * @param format 格式
     * @return minus value indicate error
     */
    public synchronized int addTrack(final MediaFormat format) {
        if (mIsStarted){
            throw new IllegalStateException("muxer already started");
        }
        final int trackIx = mMediaMuxer.addTrack(format);
        if (DEBUG){
            Log.i(TAG, "addTrack:trackNum=" + mEncoderCount + ",trackIx=" + trackIx + ",format=" + format);
        }
        return trackIx;
    }
    /**
     * write encoded data to muxer
     * @param trackIndex  track
     * @param byteBuf byte
     * @param bufferInfo buffer
     */
	public synchronized void writeSampleData(final int trackIndex, final ByteBuffer byteBuf, final MediaCodec
            .BufferInfo bufferInfo) {
        if (mStatredCount > 0)
            mMediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
    }

    public void setContext(Context context) {
        WeakReference<Context> contextWeakReference = new WeakReference<Context>(context);
        this.mContext = contextWeakReference.get();
    }

    public synchronized boolean isStarted() {
        return mIsStarted;
    }

    public String getFilePath(){
        return mOutputPath;
    }
}
