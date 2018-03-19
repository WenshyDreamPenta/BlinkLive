package com.blink.live.blinkstreamlib.client;

import android.graphics.SurfaceTexture;

import com.blink.live.blinkstreamlib.core.listeners.RESScreenShotListener;
import com.blink.live.blinkstreamlib.core.listeners.RESVideoChangeListener;
import com.blink.live.blinkstreamlib.encoder.MediaVideoEncoder;
import com.blink.live.blinkstreamlib.model.RESConfig;
import com.blink.live.blinkstreamlib.model.RESCoreParameters;
import com.blink.live.blinkstreamlib.rtmp.RESFlvDataCollecter;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/19
 *     desc   : Video编码工具类接口
 * </pre>
 */
public interface RESVideoCore {
    int OVERWATCH_TEXTURE_ID = 10;
    boolean prepare(RESConfig resConfig);

    void updateCamTexture(SurfaceTexture camTex);

    void startPreview(SurfaceTexture surfaceTexture, int visualWidth, int visualHeight);

    void updatePreview(int visualWidth, int visualHeight);

    void stopPreview(boolean releaseTexture);

    boolean startStreaming(RESFlvDataCollecter flvDataCollecter);

    boolean stopStreaming();

    boolean destroy();

    void reSetVideoBitrate(int bitrate);

    int getVideoBitrate();

    void reSetVideoFPS(int fps);

    void reSetVideoSize(RESCoreParameters newParameters);

    void setCurrentCamera(int cameraIndex);

    void takeScreenShot(RESScreenShotListener listener);

    void setVideoChangeListener(RESVideoChangeListener listener);

    float getDrawFrameRate();

    void setVideoEncoder(final MediaVideoEncoder encoder);

    void setMirror(boolean isEnableMirror,boolean isEnablePreviewMirror,boolean isEnableStreamMirror);
}
