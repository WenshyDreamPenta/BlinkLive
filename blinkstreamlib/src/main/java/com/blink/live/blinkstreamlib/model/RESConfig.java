package com.blink.live.blinkstreamlib.model;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/19
 *     desc   : 参数配置类
 * </pre>
 */
public class RESConfig {

    private RESConfig(){}
    /**
     * 过滤器类型
     */
    public static class FilterMode {
        public static final int HARD = RESCoreParameters.FILTER_MODE_HARD;
        public static final int SOFT = RESCoreParameters.FILTER_MODE_SOFT;
    }

    /**
     * 渲染类型
     */
    public static class RenderingMode {
        public static final int NativeWindow = RESCoreParameters.RENDERING_MODE_NATIVE_WINDOW;
        public static final int OpenGLES = RESCoreParameters.RENDERING_MODE_OPENGLES;
    }

    /**
     * 方向类型
     */
    public static class DirectionMode {
        public static final int FLAG_DIRECTION_FLIP_HORIZONTAL = RESCoreParameters.FLAG_DIRECTION_FLIP_HORIZONTAL;
        public static final int FLAG_DIRECTION_FLIP_VERTICAL = RESCoreParameters.FLAG_DIRECTION_FLIP_VERTICAL;
        public static final int FLAG_DIRECTION_ROATATION_0 = RESCoreParameters.FLAG_DIRECTION_ROATATION_0;
        public static final int FLAG_DIRECTION_ROATATION_90 = RESCoreParameters.FLAG_DIRECTION_ROATATION_90;
        public static final int FLAG_DIRECTION_ROATATION_180 = RESCoreParameters.FLAG_DIRECTION_ROATATION_180;
        public static final int FLAG_DIRECTION_ROATATION_270 = RESCoreParameters.FLAG_DIRECTION_ROATATION_270;
    }

    private int filterMode;
    private RESize targetVideoSize;
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
    private RESize targetPreviewSize;
}
