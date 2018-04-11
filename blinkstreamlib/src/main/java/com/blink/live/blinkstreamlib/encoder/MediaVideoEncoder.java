package com.blink.live.blinkstreamlib.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.opengl.EGLContext;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import com.blink.live.blinkstreamlib.utils.LogUtil;

import java.io.IOException;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/19
 *     desc   : MediaCodec Encoder
 * </pre>
 */
public class MediaVideoEncoder extends MediaEncoder {
    private static final String TAG = "MediaVideoEncoder";
    private static final String MIME_TYPE = "video/avc";
    private static final int FRAME_RATE = 24;
    private static final float BPP = 0.25f;

    private final int mWidth;
    private final int mHeight;
    private RenderHandler mRenderHandler;
    private Surface mSurface;

    private int previewW, previewH;
    private boolean isMatrixCalc = false;
    private float[] mvpMatrix = new float[]{
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1};
    private boolean isMatricCalc = false;

    public MediaVideoEncoder(MediaMuxerWrapper muxer, MediaEncoderListener mediaEncoderListener, final int width, final int height) {
        super(muxer, mediaEncoderListener);
        LogUtil.v(TAG, "MediaVideoEncoder: ");
        mWidth = width;
        mHeight = height;
        mRenderHandler = RenderHandler.createHandler(TAG);
    }

    public boolean frameAvailableSoon(final float[] tex_matrix) {
        boolean result;
        if (result = super.frameAvailableSoon()) {
            mRenderHandler.draw(tex_matrix, mvpMatrix);
        }
        return result;
    }

    public boolean frameAvailableSoon(final float[] tex_matrix, final float[] mvp_matrix) {
        boolean result;
        if (result = super.frameAvailableSoon()) {
            mRenderHandler.draw(tex_matrix, mvp_matrix);
        }
        return result;
    }

    @Override
    public boolean frameAvailableSoon() {
        boolean result;
        if (result = super.frameAvailableSoon())
            mRenderHandler.draw(null);
        return result;
    }

    @Override
    public void prepare() throws IOException {
        LogUtil.v(TAG, "prepare: ");
        mTrackIndex = -1;
        mMuxerStarted = mIsEOS = false;
        final MediaCodecInfo videoCodecInfo = selectVideoCodec(MIME_TYPE);
        if(videoCodecInfo == null){
            LogUtil.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }
        LogUtil.i(TAG, "selected codec: " + videoCodecInfo.getName());
        final MediaFormat format = MediaFormat.createAudioFormat(MIME_TYPE, mWidth, mHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);    // API >= 18
        format.setInteger(MediaFormat.KEY_BIT_RATE, calcBitRate());
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 6);

        LogUtil.i(TAG, "format: "+ format);
        mMediaCodec = MediaCodec.createDecoderByType(MIME_TYPE);
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mSurface = mMediaCodec.createInputSurface();
        mMediaCodec.start();

        LogUtil.i(TAG, "prepare finishing");
        if(mediaEncoderListener != null){
            try {
                mediaEncoderListener.onPrepared(this);
            }
            catch (Exception e) {
                LogUtil.e(TAG, "prepare: " + e);
            }
        }
    }

    private int calcBitRate() {
        final int bitrate = (int) (BPP * FRAME_RATE * mWidth * mHeight);
        Log.i(TAG, String.format("bitrate=%5.2f[Mbps]", bitrate / 1024f / 1024f));
        return bitrate;
    }

    public void setEglContext(final EGLContext shared_context, final int tex_id) {
        mRenderHandler.setEglContext(shared_context, tex_id, mSurface, true);
    }

    @Override
    protected void signalEndOfInputStream() {
        LogUtil.d(TAG, "sending EOS to encoder");
        mMediaCodec.signalEndOfInputStream();    // API >= 18
        mIsEOS = true;
    }

    @Override
    public void release() {
        LogUtil.i(TAG, "release:");
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        if (mRenderHandler != null) {
            mRenderHandler.release();
            mRenderHandler = null;
        }
        super.release();
    }

    protected static MediaCodecInfo selectVideoCodec(final String mimeType) {
        final int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    LogUtil.v(TAG, "codec:" + codecInfo.getName() + ",MIME=" + types[j]);
                    final int format = selectColorFormat(codecInfo, mimeType);
                    if (format > 0) {
                        return codecInfo;
                    }
                }
            }
        }
        return null;
    }

    /**
     * select color format available on specific codec and we can use.
     *
     * @return 0 if no colorFormat is matched
     */
    protected static final int selectColorFormat(final MediaCodecInfo codecInfo, final String mimeType) {
        LogUtil.v(TAG, "selectColorFormat: ");
        int result = 0;
        final MediaCodecInfo.CodecCapabilities caps;
        try {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            caps = codecInfo.getCapabilitiesForType(mimeType);
        }
        finally {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        }
        int colorFormat;
        for (int i = 0; i < caps.colorFormats.length; i++) {
            colorFormat = caps.colorFormats[i];
            if (isRecognizedViewoFormat(colorFormat)) {
                if (result == 0) {
                    result = colorFormat;
                }
                break;
            }
        }
        if (result == 0) {
            Log.e(TAG, "couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
        }
        return result;
    }

    /**
     * color formats that we can use in this class
     */
    protected static int[] recognizedFormats;

    static {
        recognizedFormats = new int[]{
                //        	MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
                //        	MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
                //        	MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface};
    }

    private static boolean isRecognizedViewoFormat(final int colorFormat) {
        LogUtil.v(TAG, "isRecognizedViewoFormat:colorFormat=" + colorFormat);
        final int n = recognizedFormats != null ? recognizedFormats.length : 0;
        for (int i = 0; i < n; i++) {
            if (recognizedFormats[i] == colorFormat) {
                return true;
            }
        }
        return false;
    }

    /**
     * set preview size
     *
     * @param previewW width
     * @param previewH height
     */
    public void setPreviewWH(int previewW, int previewH) {
        this.previewW = previewW;
        this.previewH = previewH;
    }

    /**
     * get mvp matrix
     *
     * @return float[] mvp matrix
     */
    public float[] getMvpMatrix() {
        if (previewW < 1 || previewH < 1)
            return null;
        if (isMatrixCalc)
            return mvpMatrix;

        float encodeWHRatio = mWidth * 1.0f / mHeight;
        float previewWHRatio = previewW * 1.0f / previewH;

        float[] projection = new float[16];
        float[] camera = new float[16];

        if (encodeWHRatio > previewWHRatio) {
            Matrix.orthoM(projection, 0, -1, 1, -previewWHRatio / encodeWHRatio, previewWHRatio / encodeWHRatio, 1, 3);
        }
        else {
            Matrix.orthoM(projection, 0, -encodeWHRatio / previewWHRatio, encodeWHRatio / previewWHRatio, -1, 1, 1, 3);
        }
        Matrix.setLookAtM(camera, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projection, 0, camera, 0);

        isMatrixCalc = true;

        return mvpMatrix;
    }
}
