package com.blink.live.blinkstreamlib.core.video;

import android.graphics.SurfaceTexture;

import com.blink.live.blinkstreamlib.core.listener.StreamScreenShotListener;
import com.blink.live.blinkstreamlib.core.listener.StreamVideoChangeListener;
import com.blink.live.blinkstreamlib.encoder.MediaVideoEncoder;
import com.blink.live.blinkstreamlib.model.StreamConfig;
import com.blink.live.blinkstreamlib.model.StreamCoreParameters;
import com.blink.live.blinkstreamlib.rtmp.StreamFlvDataCollecter;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/19
 *     desc   : Video编码工具类接口
 * </pre>
 */
public interface StreamVideoCore {
    int OVERWATCH_TEXTURE_ID = 10;
    boolean prepare(StreamConfig streamConfig);

    void updateCamTexture(SurfaceTexture camTex);

    void startPreview(SurfaceTexture surfaceTexture, int visualWidth, int visualHeight);

    void updatePreview(int visualWidth, int visualHeight);

    void stopPreview(boolean releaseTexture);

    boolean startStreaming(StreamFlvDataCollecter flvDataCollecter);

    boolean stopStreaming();

    boolean destroy();

    void reSetVideoBitrate(int bitrate);

    int getVideoBitrate();

    void reSetVideoFPS(int fps);

    void reSetVideoSize(StreamCoreParameters newParameters);

    void setCurrentCamera(int cameraIndex);

    void takeScreenShot(StreamScreenShotListener listener);

    void setVideoChangeListener(StreamVideoChangeListener listener);

    float getDrawFrameRate();

    void setVideoEncoder(final MediaVideoEncoder encoder);

    void setMirror(boolean isEnableMirror,boolean isEnablePreviewMirror,boolean isEnableStreamMirror);
}
