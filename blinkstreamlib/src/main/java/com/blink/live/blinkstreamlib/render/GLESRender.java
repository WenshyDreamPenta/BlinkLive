package com.blink.live.blinkstreamlib.render;

import android.graphics.SurfaceTexture;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/9
 *     desc   :
 * </pre>
 */
public class GLESRender implements IRender {
    @Override
    public void create(SurfaceTexture visualSurfaceTexture, int pixelFormat, int pixelWidth,
            int pixelHeight, int visualWidth, int visualHeight) {

    }

    @Override
    public void update(int visualWidth, int visualHeight) {

    }

    @Override
    public void rendering(byte[] pixel) {

    }

    @Override
    public void destroy(boolean releaseTexture) {

    }

    @SuppressWarnings("all")
    private static native void NV21TOYUV(byte[] src, byte[] dstY, byte[] dstU, byte[] dstV, int width, int height);
}
