package com.blink.live.blinkstreamlib.render;

import android.graphics.SurfaceTexture;
import android.view.Surface;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/9
 *     desc   :
 * </pre>
 */
public class NativeRender implements IRender {
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
    private native void renderingSurface(Surface surface, byte[] pixels, int w, int h, int s);
}