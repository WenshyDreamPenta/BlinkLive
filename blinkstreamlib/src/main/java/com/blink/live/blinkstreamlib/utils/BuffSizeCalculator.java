package com.blink.live.blinkstreamlib.utils;

import android.graphics.ImageFormat;
import android.media.MediaCodecInfo;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/23
 *     desc   : Buff Size Calculator
 * </pre>
 */
public class BuffSizeCalculator {
    public static int calculator(int width, int height, int colorFormat) {
        switch (colorFormat) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case ImageFormat.NV21:
            case ImageFormat.YV12:
                return width * height * 3 / 2;
            default:
                return -1;
        }
    }
}
