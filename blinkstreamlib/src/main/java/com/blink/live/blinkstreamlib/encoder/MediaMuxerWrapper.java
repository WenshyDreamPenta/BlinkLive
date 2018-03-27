package com.blink.live.blinkstreamlib.encoder;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import com.blink.live.blinkstreamlib.utils.FileUtil;

import java.io.IOException;

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

    public void setContext(Context context) {
        this.mContext = context;
    }



}
