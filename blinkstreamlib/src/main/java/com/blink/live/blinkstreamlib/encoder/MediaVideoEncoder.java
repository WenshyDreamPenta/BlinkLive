package com.blink.live.blinkstreamlib.encoder;

import android.util.Log;
import android.view.Surface;

import com.blink.live.blinkstreamlib.encoder.utils.RenderHandler;

import java.io.IOException;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/19
 *     desc   : MediaCodec Encoder
 * </pre>
 */
public class MediaVideoEncoder extends MediaEncoder{
    private static final boolean DEBUG = false;
    private static final String TAG = "MediaVideoEncoder";
    private static final String MIME_TYPE = "video/avc";
    private static final int FRAME_RATE = 24;
    private static final float BPP = 0.25f;

    private final int mWidth;
    private final int mHeight;
    private RenderHandler mRanderHandler;
    private Surface mSurface;

    private int previwW, previewH;
    private float[]  mvpMatrix = new float[]{
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
    };
    private boolean isMatricCalc = false;

    public MediaVideoEncoder(MediaMuxerWrapper muxer, MediaEncoderListener mediaEncoderListener, final int width, final int
            height) {
        super(muxer, mediaEncoderListener);
        //todo:
        if(DEBUG){
            Log.i(TAG, "MediaVideoEncoder: ");
        }
        mWidth = width;
        mHeight = height;
        mRanderHandler = RenderHandler.createHandler(TAG);
    }





    @Override
    public void run() {

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
}
