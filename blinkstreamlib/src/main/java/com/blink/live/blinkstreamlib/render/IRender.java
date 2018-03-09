package com.blink.live.blinkstreamlib.render;

import android.graphics.SurfaceTexture;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/9
 *     desc   :
 * </pre>
 */
public interface IRender {
    void create(SurfaceTexture visualSurfaceTexture, int pixelFormat, int pixelWidth,
            int pixelHeight, int visualWidth, int visualHeight);

    void update(int visualWidth, int visualHeight);

    void rendering(byte[] pixel);

    void destroy(boolean releaseTexture);
}
