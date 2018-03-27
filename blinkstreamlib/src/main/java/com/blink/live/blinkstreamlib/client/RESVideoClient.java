package com.blink.live.blinkstreamlib.client;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import com.blink.live.blinkstreamlib.core.RESHardVideoCore;
import com.blink.live.blinkstreamlib.core.RESSoftVideoCore;
import com.blink.live.blinkstreamlib.core.RESVideoCore;
import com.blink.live.blinkstreamlib.model.RESConfig;
import com.blink.live.blinkstreamlib.model.RESCoreParameters;
import com.blink.live.blinkstreamlib.model.RESize;
import com.blink.live.blinkstreamlib.tools.CameraHelper;
import com.blink.live.blinkstreamlib.utils.LogUtil;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/19
 *     desc   : Video 编码推流工具类
 * </pre>
 */
public class RESVideoClient {
    private RESCoreParameters mRESCoreParameters;
    private final Object syncOp = new Object();
    private Camera mCamera;
    public SurfaceTexture camTexture;
    private int cameraNum;
    private int curentCameraIndex;
    private RESVideoCore resVideoCore;
    private boolean isStreaming;
    private boolean isPreviewing;

    public RESVideoClient(RESCoreParameters mRESCoreParameters) {
        this.mRESCoreParameters = mRESCoreParameters;
        this.cameraNum = Camera.getNumberOfCameras();
        this.curentCameraIndex = Camera.CameraInfo.CAMERA_FACING_BACK;
        this.isStreaming = false;
        this.isPreviewing = false;
    }

    public boolean prepare(RESConfig resConfig) {
        synchronized (syncOp) {
            if ((cameraNum - 1) >= resConfig.getDefaultCamera()) {
                curentCameraIndex = resConfig.getDefaultCamera();
            }
            if (null == (mCamera = createCamera(curentCameraIndex))){
                LogUtil.e("can not open camera");
                return false;
            }
            Camera.Parameters parameters = mCamera.getParameters();
            CameraHelper.selectCameraPreviewWH(parameters, mRESCoreParameters, resConfig.getTargetPreviewSize());
            CameraHelper.selectCameraFpsRange(parameters, mRESCoreParameters);
            if (resConfig.getVideoFPS() > mRESCoreParameters.previewMaxFps / 1000) {
                mRESCoreParameters.videoFPS = mRESCoreParameters.previewMaxFps / 1000;
            } else {
                mRESCoreParameters.videoFPS = resConfig.getVideoFPS();
            }
            resolveResolution(mRESCoreParameters, resConfig.getTargetVideoSize());
            if (!CameraHelper.selectCameraColorFormat(parameters, mRESCoreParameters)) {
                LogUtil.e("CameraHelper.selectCameraColorFormat,Failed");
                mRESCoreParameters.dump();
                return false;
            }
            if (!CameraHelper.configCamera(mCamera, mRESCoreParameters)) {
                LogUtil.e("CameraHelper.configCamera,Failed");
                mRESCoreParameters.dump();
                return false;
            }
            switch (mRESCoreParameters.filterMode) {
                case RESCoreParameters.FILTER_MODE_SOFT:
                    resVideoCore = new RESSoftVideoCore(mRESCoreParameters);
                    break;
                case RESCoreParameters.FILTER_MODE_HARD:
                    resVideoCore = new RESHardVideoCore(mRESCoreParameters);
                    break;
            }
            if(!resVideoCore.prepare(resConfig)){
                return false;
            }
            resVideoCore.setCurrentCamera(curentCameraIndex);
            prepareVideo();
        } return true;
    }

    private Camera createCamera(int cameraId) {
        try {
            mCamera = Camera.open(cameraId);
            mCamera.setDisplayOrientation(0);
        }
        catch (SecurityException e) {
            LogUtil.trace("no permission", e);
            return null;
        }
        catch (Exception e) {
            LogUtil.trace("camera.open()failed", e);
            return null;
        }
        return mCamera;
    }

    private void resolveResolution(RESCoreParameters resCoreParameters, RESize targetVideoSize) {
        if (resCoreParameters.filterMode == RESCoreParameters.FILTER_MODE_SOFT) {
            if (resCoreParameters.isPortrait) {
                resCoreParameters.videoWidth = resCoreParameters.previewVideoWidth;
                resCoreParameters.videoHeight = resCoreParameters.previewVideoHeight;
            }
            else {
                resCoreParameters.videoWidth = resCoreParameters.previewVideoWidth;
                resCoreParameters.videoHeight = resCoreParameters.previewVideoHeight;
            }
        }
        else {
            float pw, ph, vw, vh;
            if (resCoreParameters.isPortrait) {
                resCoreParameters.videoHeight = targetVideoSize.getWidth();
                resCoreParameters.videoWidth = targetVideoSize.getHeight();
                pw = resCoreParameters.previewVideoHeight;
                ph = resCoreParameters.previewVideoWidth;
            }
            else {
                resCoreParameters.videoWidth = targetVideoSize.getWidth();
                resCoreParameters.videoHeight = targetVideoSize.getHeight();
                pw = resCoreParameters.previewVideoWidth;
                ph = resCoreParameters.previewVideoHeight;
            }
            vw = resCoreParameters.videoWidth;
            vh = resCoreParameters.videoHeight;
            float pr = ph / pw, vr = vh / vw;
            if (pr == vr) {
                resCoreParameters.cropRatio = 0.0f;
            }
            else if (pr > vr) {
                resCoreParameters.cropRatio = (1.0f - vr / pr) / 2.0f;
            }
            else {
                resCoreParameters.cropRatio = -(1.0f - pr / vr) / 2.0f;
            }
        }
    }

    private boolean prepareVideo(){
        if (mRESCoreParameters.filterMode == RESCoreParameters.FILTER_MODE_SOFT) {
            mCamera.addCallbackBuffer(new byte[mRESCoreParameters.previewBufferSize]);
            mCamera.addCallbackBuffer(new byte[mRESCoreParameters.previewBufferSize]);
        }
        return true;
    }

}
