package com.blink.live.blinkstreamlib.model;

import android.hardware.Camera;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/19
 *     desc   : 参数配置类
 * </pre>
 */
public class StreamConfig {

    private StreamConfig(){}
    /**
     * 过滤器类型
     */
    public static class FilterMode {
        public static final int HARD = StreamCoreParameters.FILTER_MODE_HARD;
        public static final int SOFT = StreamCoreParameters.FILTER_MODE_SOFT;
    }

    /**
     * 渲染类型
     */
    public static class RenderingMode {
        public static final int NativeWindow = StreamCoreParameters.RENDERING_MODE_NATIVE_WINDOW;
        public static final int OpenGLES = StreamCoreParameters.RENDERING_MODE_OPENGLES;
    }

    /**
     * 方向类型
     */
    public static class DirectionMode {
        public static final int FLAG_DIRECTION_FLIP_HORIZONTAL = StreamCoreParameters.FLAG_DIRECTION_FLIP_HORIZONTAL;
        public static final int FLAG_DIRECTION_FLIP_VERTICAL = StreamCoreParameters.FLAG_DIRECTION_FLIP_VERTICAL;
        public static final int FLAG_DIRECTION_ROATATION_0 = StreamCoreParameters.FLAG_DIRECTION_ROATATION_0;
        public static final int FLAG_DIRECTION_ROATATION_90 = StreamCoreParameters.FLAG_DIRECTION_ROATATION_90;
        public static final int FLAG_DIRECTION_ROATATION_180 = StreamCoreParameters.FLAG_DIRECTION_ROATATION_180;
        public static final int FLAG_DIRECTION_ROATATION_270 = StreamCoreParameters.FLAG_DIRECTION_ROATATION_270;
    }

    private int filterMode;
    private Size targetVideoSize;
    private int videoBufferQueueNum;
    private int bitRate;
    private String rtmpAddr;
    private int renderingMode;
    private int defaultCamera;
    private int frontCameraDirectionMode;
    private int backCameraDirectionMode;
    private int videoFPS;
    private int videoGOP;
    private boolean printDetailMsg;
    private Size targetPreviewSize;

    public static StreamConfig obtain() {
        StreamConfig res = new StreamConfig();
        res.setFilterMode(FilterMode.SOFT);
        res.setRenderingMode(RenderingMode.NativeWindow);
        res.setTargetVideoSize(new Size(1280, 720));
        res.setVideoFPS(15);
        res.setVideoGOP(2);
        res.setVideoBufferQueueNum(5);
        res.setBitRate(2000000);
        res.setPrintDetailMsg(false);
        res.setDefaultCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
        res.setBackCameraDirectionMode(DirectionMode.FLAG_DIRECTION_ROATATION_0);
        res.setFrontCameraDirectionMode(DirectionMode.FLAG_DIRECTION_ROATATION_0);
        return res;
    }

    public int getFilterMode() {
        return filterMode;
    }

    public void setFilterMode(int filterMode) {
        this.filterMode = filterMode;
    }

    public Size getTargetVideoSize() {
        return targetVideoSize;
    }

    public void setTargetVideoSize(Size targetVideoSize) {
        this.targetVideoSize = targetVideoSize;
    }

    public int getVideoBufferQueueNum() {
        return videoBufferQueueNum;
    }

    public void setVideoBufferQueueNum(int videoBufferQueueNum) {
        this.videoBufferQueueNum = videoBufferQueueNum;
    }

    public int getBitRate() {
        return bitRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public String getRtmpAddr() {
        return rtmpAddr;
    }

    public void setRtmpAddr(String rtmpAddr) {
        this.rtmpAddr = rtmpAddr;
    }

    public int getRenderingMode() {
        return renderingMode;
    }

    public void setRenderingMode(int renderingMode) {
        this.renderingMode = renderingMode;
    }

    public int getDefaultCamera() {
        return defaultCamera;
    }

    public void setDefaultCamera(int defaultCamera) {
        this.defaultCamera = defaultCamera;
    }

    public int getFrontCameraDirectionMode() {
        return frontCameraDirectionMode;
    }

    public void setFrontCameraDirectionMode(int frontCameraDirectionMode) {
        this.frontCameraDirectionMode = frontCameraDirectionMode;
    }

    public int getBackCameraDirectionMode() {
        return backCameraDirectionMode;
    }

    public void setBackCameraDirectionMode(int backCameraDirectionMode) {
        this.backCameraDirectionMode = backCameraDirectionMode;
    }

    public int getVideoFPS() {
        return videoFPS;
    }

    public void setVideoFPS(int videoFPS) {
        this.videoFPS = videoFPS;
    }

    public int getVideoGOP() {
        return videoGOP;
    }

    public void setVideoGOP(int videoGOP) {
        this.videoGOP = videoGOP;
    }

    public boolean isPrintDetailMsg() {
        return printDetailMsg;
    }

    public void setPrintDetailMsg(boolean printDetailMsg) {
        this.printDetailMsg = printDetailMsg;
    }

    public Size getTargetPreviewSize() {
        return targetPreviewSize;
    }

    public void setTargetPreviewSize(Size targetPreviewSize) {
        this.targetPreviewSize = targetPreviewSize;
    }
}
