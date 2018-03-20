package com.blink.live.blinkstreamlib.core;

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
 *     time   : 2018/3/20
 *     desc   :
 * </pre>
 */
public class RESHardVideoCore implements RESVideoCore{
    private RESCoreParameters resCoreParameters;

    public RESHardVideoCore(RESCoreParameters resCoreParameters) {
        this.resCoreParameters = resCoreParameters;
    }

    @Override
    public boolean prepare(RESConfig resConfig) {
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
    public boolean startStreaming(RESFlvDataCollecter flvDataCollecter) {
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
    public void reSetVideoSize(RESCoreParameters newParameters) {

    }

    @Override
    public void setCurrentCamera(int cameraIndex) {

    }

    @Override
    public void takeScreenShot(RESScreenShotListener listener) {

    }

    @Override
    public void setVideoChangeListener(RESVideoChangeListener listener) {

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
