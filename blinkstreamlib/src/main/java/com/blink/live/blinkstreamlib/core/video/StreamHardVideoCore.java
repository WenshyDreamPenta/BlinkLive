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
 *     time   : 2018/3/20
 *     desc   :
 * </pre>
 */
public class StreamHardVideoCore implements StreamVideoCore {
    private StreamCoreParameters streamCoreParameters;

    public StreamHardVideoCore(StreamCoreParameters streamCoreParameters) {
        this.streamCoreParameters = streamCoreParameters;
    }

    @Override
    public boolean prepare(StreamConfig streamConfig) {
        //todo:
        return false;
    }

    @Override
    public void updateCamTexture(SurfaceTexture camTex) {

    }

    @Override
    public void startPreview(SurfaceTexture surfaceTexture, int visualWidth, int visualHeight) {

    }

    @Override
    public void updatePreview(int visualWidth, int visualHeight) {

    }

    @Override
    public void stopPreview(boolean releaseTexture) {

    }

    @Override
    public boolean startStreaming(StreamFlvDataCollecter flvDataCollecter) {
        return false;
    }

    @Override
    public boolean stopStreaming() {
        return false;
    }

    @Override
    public boolean destroy() {
        return false;
    }

    @Override
    public void reSetVideoBitrate(int bitrate) {

    }

    @Override
    public int getVideoBitrate() {
        return 0;
    }

    @Override
    public void reSetVideoFPS(int fps) {

    }

    @Override
    public void reSetVideoSize(StreamCoreParameters newParameters) {

    }

    @Override
    public void setCurrentCamera(int cameraIndex) {

    }

    @Override
    public void takeScreenShot(StreamScreenShotListener listener) {

    }

    @Override
    public void setVideoChangeListener(StreamVideoChangeListener listener) {

    }

    @Override
    public float getDrawFrameRate() {
        return 0;
    }

    @Override
    public void setVideoEncoder(MediaVideoEncoder encoder) {

    }

    @Override
    public void setMirror(boolean isEnableMirror, boolean isEnablePreviewMirror,
            boolean isEnableStreamMirror) {

    }
}
