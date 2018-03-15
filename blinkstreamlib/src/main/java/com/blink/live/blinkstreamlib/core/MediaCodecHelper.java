package com.blink.live.blinkstreamlib.core;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import com.blink.live.blinkstreamlib.model.RESCoreParameters;
import com.blink.live.blinkstreamlib.utils.LogTools;

import java.io.IOException;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/15
 *     desc   : MediaCodec Helper
 * </pre>
 */
public class MediaCodecHelper {

    /**
     * 创建video软编码
     *
     * @param coreParameters 编码参数
     * @param videoFormat    视频格式
     * @return MediaCodec对象
     */
    public static MediaCodec createSoftVideoMediaCodec(RESCoreParameters coreParameters, MediaFormat videoFormat) {
        videoFormat.setString(MediaFormat.KEY_MIME, "video/avc");
        videoFormat.setInteger(MediaFormat.KEY_WIDTH, coreParameters.videoWidth);
        videoFormat.setInteger(MediaFormat.KEY_HEIGHT, coreParameters.videoHeight);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, coreParameters.mediacdoecAVCBitRate);
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, coreParameters.mediacodecAVCFrameRate);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, coreParameters.mediacodecAVCIFrameInterval);
        videoFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
        videoFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel31);
        videoFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
        MediaCodec result;
        try {
            result = MediaCodec.createEncoderByType(videoFormat.getString(MediaFormat.KEY_MIME));
            int[] colorful = result.getCodecInfo()
                    .getCapabilitiesForType(videoFormat.getString(MediaFormat.KEY_MIME)).colorFormats;
            int dstVideoColorFormat = -1;
            //select mediacodec colorformat
            if (isArrayContain(colorful, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)) {
                dstVideoColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
                coreParameters.mediacodecAVCColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
            }
            if (dstVideoColorFormat == -1 &&
                    isArrayContain(colorful, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar)) {
                dstVideoColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
                coreParameters.mediacodecAVCColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
            }
            if (dstVideoColorFormat == -1) {
                LogTools.e("UnSupport,mediaCodecColorFormat");
                return null;
            }
            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, dstVideoColorFormat);
        }
        catch (Exception e) {
            LogTools.trace(e);
            return null;
        }
        return result;

    }

    /**
     * 创建Audio软编码
     *
     * @param coreParameters 编码参数
     * @param audioFormat    音频格式
     * @return MediaCodec对象
     */
    public static MediaCodec createAudioMediaCodec(RESCoreParameters coreParameters, MediaFormat audioFormat) {
        //Audio
        MediaCodec result;
        audioFormat.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, coreParameters.mediacodecAACProfile);
        audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, coreParameters.mediacodecAACSampleRate);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, coreParameters.mediacodecAACChannelCount);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, coreParameters.mediacodecAACBitRate);
        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, coreParameters.mediacodecAACMaxInputSize);
        LogTools.d("creatingAudioEncoder,format=" + audioFormat.toString());
        try {
            result = MediaCodec.createEncoderByType(audioFormat.getString(MediaFormat.KEY_MIME));
        }
        catch (Exception e) {
            LogTools.trace("can`t create audioEncoder!", e);
            return null;
        }
        return result;
    }

    /**
     * 创建video硬编码
     *
     * @param coreParameters 编码参数
     * @param videoFormat    视频格式
     * @return MediaCodec对象
     */
    public static MediaCodec createHardVideoMediaCodec(RESCoreParameters coreParameters, MediaFormat videoFormat) {
        videoFormat.setString(MediaFormat.KEY_MIME, "video/avc");
        videoFormat.setInteger(MediaFormat.KEY_WIDTH, coreParameters.videoWidth);
        videoFormat.setInteger(MediaFormat.KEY_HEIGHT, coreParameters.videoHeight);
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, coreParameters.mediacdoecAVCBitRate);
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, coreParameters.mediacodecAVCFrameRate);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, coreParameters.mediacodecAVCIFrameInterval);
        videoFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
        videoFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel31);
        videoFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
        videoFormat.setInteger(MediaFormat.KEY_COMPLEXITY, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);//added by wangshuo
        MediaCodec result;
        try {
            result = MediaCodec.createEncoderByType(videoFormat.getString(MediaFormat.KEY_MIME));
        } catch (IOException e) {
            LogTools.trace(e);
            return null;
        }
        return result;
    }
    private static boolean isArrayContain(int[] src, int target) {
        for (int color : src) {
            if (color == target) {
                return true;
            }
        }
        return false;
    }

}
