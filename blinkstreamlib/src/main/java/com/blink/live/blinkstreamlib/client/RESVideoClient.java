package com.blink.live.blinkstreamlib.client;

import android.graphics.Camera;
import android.graphics.SurfaceTexture;

import com.blink.live.blinkstreamlib.model.RESCoreParameters;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/19
 *     desc   : Video 编码推流工具类
 * </pre>
 */
public class RESVideoClient {
    RESCoreParameters mRESCoreParameters;
    private final Object syncOp = new Object();
    private Camera mCamera;
    public SurfaceTexture camTexture;
    private int cameraNum;
    private int curentCameraIndex;


}
