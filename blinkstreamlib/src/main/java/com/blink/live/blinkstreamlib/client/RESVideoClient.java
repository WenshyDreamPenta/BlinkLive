package com.blink.live.blinkstreamlib.client;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import com.blink.live.blinkstreamlib.core.RESHardVideoCore;
import com.blink.live.blinkstreamlib.core.RESSoftVideoCore;
import com.blink.live.blinkstreamlib.core.RESVideoCore;
import com.blink.live.blinkstreamlib.model.RESConfig;
import com.blink.live.blinkstreamlib.model.RESCoreParameters;
import com.blink.live.blinkstreamlib.model.RESize;
import com.blink.live.blinkstreamlib.rtmp.RESFlvDataCollecter;
import com.blink.live.blinkstreamlib.tools.BuffSizeCalculator;
import com.blink.live.blinkstreamlib.tools.CameraTools;
import com.blink.live.blinkstreamlib.utils.LogUtil;

import java.util.List;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/19
 *     desc   : Video 编码推流工具类
 * </pre>
 */
public class RESVideoClient {
    public RESVideoClient videoClient;

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
            CameraTools.selectCameraPreviewWH(parameters, mRESCoreParameters, resConfig.getTargetPreviewSize());
            CameraTools.selectCameraFpsRange(parameters, mRESCoreParameters);
            if (resConfig.getVideoFPS() > mRESCoreParameters.previewMaxFps / 1000) {
                mRESCoreParameters.videoFPS = mRESCoreParameters.previewMaxFps / 1000;
            } else {
                mRESCoreParameters.videoFPS = resConfig.getVideoFPS();
            }
            resolveResolution(mRESCoreParameters, resConfig.getTargetVideoSize());
            if (!CameraTools.selectCameraColorFormat(parameters, mRESCoreParameters)) {
                LogUtil.e("CameraTools.selectCameraColorFormat,Failed");
                mRESCoreParameters.dump();
                return false;
            }
            if (!CameraTools.configCamera(mCamera, mRESCoreParameters)) {
                LogUtil.e("CameraTools.configCamera,Failed");
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

    //start Video codec
    private boolean startVideo(){
        camTexture = new SurfaceTexture(RESVideoCore.OVERWATCH_TEXTURE_ID);
        if(mRESCoreParameters.filterMode == RESCoreParameters.FILTER_MODE_SOFT){
            //soft codec
            mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    synchronized (syncOp){
                        if(resVideoCore != null && data != null){
                            ((RESSoftVideoCore) resVideoCore).queueVideo(data);
                        }
                    }
                }
            });
        }
        else{
            camTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                    //todo： hard codec
                }
            });
        }
        try{
            mCamera.setPreviewTexture(camTexture);
        }catch (Exception e){
            LogUtil.trace(e);
            mCamera.release();
            return false;
        }

        mCamera.startPreview();
        return true;
    }

    public boolean startPreview(SurfaceTexture surfaceTexture, int visualWidth, int visualHeight){
        synchronized (syncOp){
            if(!isStreaming && !isPreviewing){
                if(!startVideo()){
                    mRESCoreParameters.dump();
                    LogUtil.e("RESVideoClient,start(),failed");
                    return false;
                }
                resVideoCore.updateCamTexture(camTexture);
            }
            resVideoCore.startPreview(surfaceTexture, visualWidth, visualHeight);
            isPreviewing = true;

            return true;
        }
    }

    public void updatePreview(int visualWidth, int visualHeight){
        resVideoCore.updatePreview(visualWidth, visualHeight);
    }

    public boolean stopPreview(boolean releaseTexture){
        synchronized (syncOp){
            if(isPreviewing){
                resVideoCore.stopPreview(releaseTexture);
                if(!isStreaming){
                    mCamera.stopPreview();
                    resVideoCore.updateCamTexture(null);
                    camTexture.release();
                }
            }
            isPreviewing = false;
            return true;
        }
    }

    public boolean startStreaming(RESFlvDataCollecter flvDataCollecter){
        synchronized (syncOp){
            if (!isStreaming && !isPreviewing) {
                if (!startVideo()) {
                    mRESCoreParameters.dump();
                    LogUtil.e("RESVideoClient,start(),failed");
                    return false;
                }
                resVideoCore.updateCamTexture(camTexture);
            }
            resVideoCore.startStreaming(flvDataCollecter);
            isStreaming = true;
            return true;
        }
    }

    public boolean stopStreaming(){
        if (isStreaming) {
            resVideoCore.stopStreaming();
            if (!isPreviewing) {
                mCamera.stopPreview();
                resVideoCore.updateCamTexture(null);
                camTexture.release();
            }
        }
        isStreaming = false;
        return true;
    }

    public boolean destroy(){
        synchronized (syncOp){
            mCamera.release();
            resVideoCore.destroy();
            resVideoCore = null;
            mCamera = null;
            return true;
        }
    }

    public boolean swtichCamera(){
        synchronized (syncOp){
            mCamera.stopPreview();
            mCamera.release();
            mCamera  = null;
            if(null == (mCamera = createCamera(curentCameraIndex = (++curentCameraIndex)%
                    cameraNum))){
                return false;
            }
            resVideoCore.setCurrentCamera(curentCameraIndex);
            CameraTools.selectCameraFpsRange(mCamera.getParameters(), mRESCoreParameters);
            if(!CameraTools.configCamera(mCamera, mRESCoreParameters)){
                mCamera.release();
                return false;
            }
            prepareVideo();
            camTexture.release();
            resVideoCore.updateCamTexture(null);
            startVideo();
            resVideoCore.updateCamTexture(camTexture);
            return true;
        }
    }

    public boolean toggleFlashLight(){
        synchronized (syncOp){
            try{
                Camera.Parameters parameters = mCamera.getParameters();
                List<String> flashModes = parameters.getSupportedFlashModes();
                String flashMode = parameters.getFlashMode();
                if(!Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode)){
                    if(flashMode.contains(Camera.Parameters.FLASH_MODE_TORCH)){
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        mCamera.setParameters(parameters);
                        return true;
                    }
                }
                else if(!Camera.Parameters.FLASH_MODE_OFF.equals(flashMode)){
                    if(flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)){
                        parameters.setFlashMode(Camera.Parameters.ANTIBANDING_OFF);
                        mCamera.setParameters(parameters);
                        return true;
                    }
                }
            }catch(Exception e){
                return false;
            }
            return false;
        }
    }
    //todo: setZoomByPercent

    /**
     * 设置缩放百分比
     * @param targetPercent 百分比数值
     * @return true/false;
     */
    public boolean setZoomByPercent(float targetPercent){
        synchronized (syncOp){
            targetPercent = Math.min(Math.max(0f, targetPercent), 1f);
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setZoom((int) (parameters.getMaxZoom() * targetPercent));
            mCamera.setParameters(parameters);

            return true;
        }
    }

    /**
     * 重置视频码率
     * @param bitrate 码率值
     */
    public void reSetVideoBitrate(int bitrate){
        synchronized (syncOp){
            if(resVideoCore != null){
                resVideoCore.reSetVideoBitrate(bitrate);
            }
        }
    }

    /**
     * 重置视频FPS
     * @param fps fps数值
     */
    public void reSetVideoFPS(int fps){
        synchronized (syncOp){
            int targetFps;
            if(fps > mRESCoreParameters.previewMaxFps / 1000){
                targetFps = mRESCoreParameters.previewMaxFps / 1000;
            }
            else {
                targetFps = fps;
            }
            if(resVideoCore != null){
                resVideoCore.reSetVideoFPS(targetFps);
            }
        }
    }

    /**
     * 重置视频尺寸
     * @param targetSize 目标尺寸
     * @return boolean
     */
    public boolean reSetVideoSize(RESize targetSize) {
        synchronized (syncOp) {
            RESCoreParameters newParameters = new RESCoreParameters();
            newParameters.isPortrait = mRESCoreParameters.isPortrait;
            newParameters.filterMode = mRESCoreParameters.filterMode;
            Camera.Parameters parameters = mCamera.getParameters();
            CameraTools.selectCameraPreviewWH(parameters, newParameters, targetSize);
            resolveResolution(newParameters, targetSize);
            boolean needRestartCamera = (
                    newParameters.previewVideoHeight != mRESCoreParameters.previewVideoHeight ||
                            newParameters.previewVideoWidth !=
                                    mRESCoreParameters.previewVideoWidth);
            if (needRestartCamera) {
                newParameters.previewBufferSize = BuffSizeCalculator.calculator(mRESCoreParameters.previewVideoWidth,
                        mRESCoreParameters.previewVideoHeight, mRESCoreParameters.previewColorFormat);
                mRESCoreParameters.previewVideoWidth = newParameters.previewVideoWidth;
                mRESCoreParameters.previewVideoHeight = newParameters.previewVideoHeight;
                mRESCoreParameters.previewBufferSize  = newParameters.previewBufferSize;
                if((isStreaming || isPreviewing)){
                    mCamera.stopPreview();
                    mCamera.release();
                    mCamera = null;
                    if(null == (createCamera(curentCameraIndex))){
                        return false;
                    }
                    if(!CameraTools.configCamera(mCamera, mRESCoreParameters)){
                        mCamera.release();
                        return false;
                    }
                    prepareVideo();
                    resVideoCore.updateCamTexture(null);
                    camTexture.release();
                    startVideo();
                    resVideoCore.updateCamTexture(camTexture);
                }
            }
            resVideoCore.reSetVideoSize(newParameters);
            return true;
        }
    }

}
