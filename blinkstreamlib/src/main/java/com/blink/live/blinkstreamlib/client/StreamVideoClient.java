package com.blink.live.blinkstreamlib.client;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import com.blink.live.blinkstreamlib.core.video.StreamHardVideoCore;
import com.blink.live.blinkstreamlib.core.video.StreamSoftVideoCore;
import com.blink.live.blinkstreamlib.core.video.StreamVideoCore;
import com.blink.live.blinkstreamlib.core.listener.StreamScreenShotListener;
import com.blink.live.blinkstreamlib.core.listener.StreamVideoChangeListener;
import com.blink.live.blinkstreamlib.encoder.MediaVideoEncoder;
import com.blink.live.blinkstreamlib.filter.BaseSoftVideoFilter;
import com.blink.live.blinkstreamlib.model.StreamConfig;
import com.blink.live.blinkstreamlib.model.StreamCoreParameters;
import com.blink.live.blinkstreamlib.model.Size;
import com.blink.live.blinkstreamlib.rtmp.StreamFlvDataCollecter;
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
public class StreamVideoClient {
    public StreamVideoClient videoClient;
    private StreamCoreParameters mStreamCoreParameters;
    private final Object syncOp = new Object();
    private Camera mCamera;
    private SurfaceTexture camTexture;
    private int cameraNum;
    private int curentCameraIndex;
    private StreamVideoCore streamVideoCore;
    private boolean isStreaming;
    private boolean isPreviewing;

    public StreamVideoClient(StreamCoreParameters mStreamCoreParameters) {
        this.mStreamCoreParameters = mStreamCoreParameters;
        this.cameraNum = Camera.getNumberOfCameras();
        this.curentCameraIndex = Camera.CameraInfo.CAMERA_FACING_BACK;
        this.isStreaming = false;
        this.isPreviewing = false;
    }

    /**
     * 参数设置
     * @param streamConfig 配置
     * @return boolean
     */
    public boolean prepare(StreamConfig streamConfig) {
        synchronized (syncOp) {
            if ((cameraNum - 1) >= streamConfig.getDefaultCamera()) {
                curentCameraIndex = streamConfig.getDefaultCamera();
            }
            if (null == (mCamera = createCamera(curentCameraIndex))) {
                LogUtil.e("can not open camera");
                return false;
            }
            Camera.Parameters parameters = mCamera.getParameters();
            CameraTools.selectCameraPreviewWH(parameters, mStreamCoreParameters, streamConfig.getTargetPreviewSize());
            CameraTools.selectCameraFpsRange(parameters, mStreamCoreParameters);
            if (streamConfig.getVideoFPS() > mStreamCoreParameters.previewMaxFps / 1000) {
                mStreamCoreParameters.videoFPS = mStreamCoreParameters.previewMaxFps / 1000;
            }
            else {
                mStreamCoreParameters.videoFPS = streamConfig.getVideoFPS();
            }
            resolveResolution(mStreamCoreParameters, streamConfig.getTargetVideoSize());
            if (!CameraTools.selectCameraColorFormat(parameters, mStreamCoreParameters)) {
                LogUtil.e("CameraTools.selectCameraColorFormat,Failed");
                mStreamCoreParameters.dump();
                return false;
            }
            if (!CameraTools.configCamera(mCamera, mStreamCoreParameters)) {
                LogUtil.e("CameraTools.configCamera,Failed");
                mStreamCoreParameters.dump();
                return false;
            }
            switch (mStreamCoreParameters.filterMode) {
                case StreamCoreParameters.FILTER_MODE_SOFT:
                    streamVideoCore = new StreamSoftVideoCore(mStreamCoreParameters);
                    break;
                case StreamCoreParameters.FILTER_MODE_HARD:
                    streamVideoCore = new StreamHardVideoCore(mStreamCoreParameters);
                    break;
            }
            if (!streamVideoCore.prepare(streamConfig)) {
                return false;
            }
            streamVideoCore.setCurrentCamera(curentCameraIndex);
            prepareVideo();
        }
        return true;
    }

    /**
     * 推流准备
     * @return boolean
     */
    private boolean prepareVideo(){
        if (mStreamCoreParameters.filterMode == StreamCoreParameters.FILTER_MODE_SOFT) {
            mCamera.addCallbackBuffer(new byte[mStreamCoreParameters.previewBufferSize]);
            mCamera.addCallbackBuffer(new byte[mStreamCoreParameters.previewBufferSize]);
        }
        return true;
    }

    /**
     * 开始推流
     * @return boolean
     */
    private boolean startVideo(){
        camTexture = new SurfaceTexture(StreamVideoCore.OVERWATCH_TEXTURE_ID);
        if(mStreamCoreParameters.filterMode == StreamCoreParameters.FILTER_MODE_SOFT){
            //soft codec
            mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    synchronized (syncOp){
                        if(streamVideoCore != null && data != null){
                            ((StreamSoftVideoCore) streamVideoCore).queueVideo(data);
                        }
                    }
                }
            });
        }
        else{
            //todo： hard codec
            camTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {

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

    /**
     * 开始预览
     * @param surfaceTexture surfaceTexture实例
     * @param visualWidth 宽度
     * @param visualHeight 高度
     * @return boolean
     */
    public boolean startPreview(SurfaceTexture surfaceTexture, int visualWidth, int visualHeight){
        synchronized (syncOp){
            if(!isStreaming && !isPreviewing){
                if(!startVideo()){
                    mStreamCoreParameters.dump();
                    LogUtil.e("StreamVideoClient,start(),failed");
                    return false;
                }
                streamVideoCore.updateCamTexture(camTexture);
            }
            streamVideoCore.startPreview(surfaceTexture, visualWidth, visualHeight);
            isPreviewing = true;

            return true;
        }
    }

    /**
     * 刷新预览
     * @param visualWidth 宽度
     * @param visualHeight 高度
     */
    public void updatePreview(int visualWidth, int visualHeight){
        streamVideoCore.updatePreview(visualWidth, visualHeight);
    }

    /**
     * 停止预览
     * @param releaseTexture 是否释放Texture
     * @return boolean
     */
    public boolean stopPreview(boolean releaseTexture){
        synchronized (syncOp){
            if(isPreviewing){
                streamVideoCore.stopPreview(releaseTexture);
                if(!isStreaming){
                    mCamera.stopPreview();
                    streamVideoCore.updateCamTexture(null);
                    camTexture.release();
                }
            }
            isPreviewing = false;
            return true;
        }
    }

    /**
     * 开始推流
     * @param flvDataCollecter flv数据回调接口
     * @return boolean
     */
    public boolean startStreaming(StreamFlvDataCollecter flvDataCollecter){
        synchronized (syncOp){
            if (!isStreaming && !isPreviewing) {
                if (!startVideo()) {
                    mStreamCoreParameters.dump();
                    LogUtil.e("StreamVideoClient,start(),failed");
                    return false;
                }
                streamVideoCore.updateCamTexture(camTexture);
            }
            streamVideoCore.startStreaming(flvDataCollecter);
            isStreaming = true;
            return true;
        }
    }

    /**
     * 停止推流
     * @return boolean
     */
    public boolean stopStreaming(){
        if (isStreaming) {
            streamVideoCore.stopStreaming();
            if (!isPreviewing) {
                mCamera.stopPreview();
                streamVideoCore.updateCamTexture(null);
                camTexture.release();
            }
        }
        isStreaming = false;
        return true;
    }

    /**
     * 释放资源
     * @return boolean
     */
    public boolean destroy(){
        synchronized (syncOp){
            mCamera.release();
            streamVideoCore.destroy();
            streamVideoCore = null;
            mCamera = null;
            return true;
        }
    }

    /**
     * 切换摄像头
     * @return boolean
     */
    public boolean swtichCamera(){
        synchronized (syncOp){
            mCamera.stopPreview();
            mCamera.release();
            mCamera  = null;
            if(null == (mCamera = createCamera(curentCameraIndex = (++curentCameraIndex)%
                    cameraNum))){
                return false;
            }
            streamVideoCore.setCurrentCamera(curentCameraIndex);
            CameraTools.selectCameraFpsRange(mCamera.getParameters(), mStreamCoreParameters);
            if(!CameraTools.configCamera(mCamera, mStreamCoreParameters)){
                mCamera.release();
                return false;
            }
            prepareVideo();
            camTexture.release();
            streamVideoCore.updateCamTexture(null);
            startVideo();
            streamVideoCore.updateCamTexture(camTexture);
            return true;
        }
    }

    /**
     * 闪光灯
     * @return boolean
     */
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
            if(streamVideoCore != null){
                streamVideoCore.reSetVideoBitrate(bitrate);
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
            if(fps > mStreamCoreParameters.previewMaxFps / 1000){
                targetFps = mStreamCoreParameters.previewMaxFps / 1000;
            }
            else {
                targetFps = fps;
            }
            if(streamVideoCore != null){
                streamVideoCore.reSetVideoFPS(targetFps);
            }
        }
    }

    /**
     * 重置视频尺寸
     * @param targetSize 目标尺寸
     * @return boolean
     */
    public boolean reSetVideoSize(Size targetSize) {
        synchronized (syncOp) {
            StreamCoreParameters newParameters = new StreamCoreParameters();
            newParameters.isPortrait = mStreamCoreParameters.isPortrait;
            newParameters.filterMode = mStreamCoreParameters.filterMode;
            Camera.Parameters parameters = mCamera.getParameters();
            CameraTools.selectCameraPreviewWH(parameters, newParameters, targetSize);
            resolveResolution(newParameters, targetSize);
            boolean needRestartCamera = (
                    newParameters.previewVideoHeight != mStreamCoreParameters.previewVideoHeight ||
                            newParameters.previewVideoWidth !=
                                    mStreamCoreParameters.previewVideoWidth);
            if (needRestartCamera) {
                newParameters.previewBufferSize = BuffSizeCalculator.calculator(mStreamCoreParameters.previewVideoWidth,
                        mStreamCoreParameters.previewVideoHeight, mStreamCoreParameters.previewColorFormat);
                mStreamCoreParameters.previewVideoWidth = newParameters.previewVideoWidth;
                mStreamCoreParameters.previewVideoHeight = newParameters.previewVideoHeight;
                mStreamCoreParameters.previewBufferSize  = newParameters.previewBufferSize;
                if((isStreaming || isPreviewing)){
                    mCamera.stopPreview();
                    mCamera.release();
                    mCamera = null;
                    if(null == (createCamera(curentCameraIndex))){
                        return false;
                    }
                    if(!CameraTools.configCamera(mCamera, mStreamCoreParameters)){
                        mCamera.release();
                        return false;
                    }
                    prepareVideo();
                    streamVideoCore.updateCamTexture(null);
                    camTexture.release();
                    startVideo();
                    streamVideoCore.updateCamTexture(camTexture);
                }
            }
            streamVideoCore.reSetVideoSize(newParameters);
            return true;
        }
    }

    /**
     * 获取滤镜
     * @return 滤镜
     */
    public BaseSoftVideoFilter acquireSoftVideoFilter(){
        if(mStreamCoreParameters.filterMode == StreamCoreParameters.FILTER_MODE_SOFT){
            return ((StreamSoftVideoCore) streamVideoCore).acquireVideoFilter();
        }
        return null;
    }

    /**
     * 释放滤镜
     */
    public void releaseSoftVideoFilter(){
        if(mStreamCoreParameters.filterMode == StreamCoreParameters.FILTER_MODE_SOFT){
            ((StreamSoftVideoCore) streamVideoCore).releaseVideoFilter();
        }
    }

    /**
     * 设置滤镜
     */
    public void setSoftVideoFilter(BaseSoftVideoFilter baseSoftVideoFilter) {
        if (mStreamCoreParameters.filterMode == StreamCoreParameters.FILTER_MODE_SOFT) {
            ((StreamSoftVideoCore) streamVideoCore).setVideoFilter(baseSoftVideoFilter);
        }
    }

    /**
     *截屏
     */
    public void takeScreenShot(StreamScreenShotListener listener){
        synchronized (syncOp){
            if(streamVideoCore != null){
                streamVideoCore.takeScreenShot(listener);
            }
        }
    }

    public void setVideoChangeListener(StreamVideoChangeListener listener) {
        synchronized (syncOp) {
            if (streamVideoCore != null) {
                streamVideoCore.setVideoChangeListener(listener);
            }
        }
    }

    /**
     * 获取帧率
     * @return 帧率
     */
    public float getDrawFrameRate() {
        synchronized (syncOp) {
            return streamVideoCore == null ? 0 : streamVideoCore.getDrawFrameRate();
        }
    }

    /**
     * 设置视频编码器
     * @param encoder 编码器
     */
    public void setVideoEncoder(final MediaVideoEncoder encoder) {
        streamVideoCore.setVideoEncoder(encoder);
    }

    /**
     * 设置镜像
     * @param isEnableMirror 是否镜像
     * @param isEnablePreviewMirror 是否镜像预览
     * @param isEnableStreamMirror 是否推镜像流
     */
    public void setMirror(boolean isEnableMirror,boolean isEnablePreviewMirror,boolean isEnableStreamMirror) {
        streamVideoCore.setMirror(isEnableMirror,isEnablePreviewMirror,isEnableStreamMirror);
    }

    /**
     * 尺寸换算
     * @param streamCoreParameters 视频参数对象
     * @param targetVideoSize 目标视频尺寸
     */
    private void resolveResolution(StreamCoreParameters streamCoreParameters, Size targetVideoSize) {
        if (streamCoreParameters.filterMode == StreamCoreParameters.FILTER_MODE_SOFT) {
            if (streamCoreParameters.isPortrait) {
                streamCoreParameters.videoWidth = streamCoreParameters.previewVideoWidth;
                streamCoreParameters.videoHeight = streamCoreParameters.previewVideoHeight;
            }
            else {
                streamCoreParameters.videoWidth = streamCoreParameters.previewVideoWidth;
                streamCoreParameters.videoHeight = streamCoreParameters.previewVideoHeight;
            }
        }
        else {
            float pw, ph, vw, vh;
            if (streamCoreParameters.isPortrait) {
                streamCoreParameters.videoHeight = targetVideoSize.getWidth();
                streamCoreParameters.videoWidth = targetVideoSize.getHeight();
                pw = streamCoreParameters.previewVideoHeight;
                ph = streamCoreParameters.previewVideoWidth;
            }
            else {
                streamCoreParameters.videoWidth = targetVideoSize.getWidth();
                streamCoreParameters.videoHeight = targetVideoSize.getHeight();
                pw = streamCoreParameters.previewVideoWidth;
                ph = streamCoreParameters.previewVideoHeight;
            }
            vw = streamCoreParameters.videoWidth;
            vh = streamCoreParameters.videoHeight;
            float pr = ph / pw, vr = vh / vw;
            if (pr == vr) {
                streamCoreParameters.cropRatio = 0.0f;
            }
            else if (pr > vr) {
                streamCoreParameters.cropRatio = (1.0f - vr / pr) / 2.0f;
            }
            else {
                streamCoreParameters.cropRatio = -(1.0f - pr / vr) / 2.0f;
            }
        }
    }

    /**
     * 根据cameraId创建Camera
     * @param cameraId cameraId
     * @return Camera实例
     */
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
}
