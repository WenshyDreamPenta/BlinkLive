package com.blink.live.blinkstreamlib.rtmp;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.blink.live.blinkstreamlib.core.RESByteSpeedometer;
import com.blink.live.blinkstreamlib.core.RESFrameRateMeter;
import com.blink.live.blinkstreamlib.core.listeners.RESConnectionListener;
import com.blink.live.blinkstreamlib.model.RESCoreParameters;
import com.blink.live.blinkstreamlib.tools.CallbackDelivery;
import com.blink.live.blinkstreamlib.utils.LogUtil;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/12
 *     desc   : Rtmp 推流
 * </pre>
 */
public class RESRtmpPusher implements IWorker {
    //时间粒度
    private static final int TIMEGRANULARITY = 3000;
    public static final int FROM_AUDIO = 8;
    public static final int FROM_VIDEO = 6;
    //工作handler线程
    private HandlerThread workHandlerThread;
    private WorkHandler workHandler;
    //同步-对象锁
    private final Object syncOp = new Object();

    //初始化参数
    public void preapare(RESCoreParameters coreParameters) {
        synchronized (syncOp) {
            workHandlerThread = new HandlerThread("RESRtmpSender,workHandlerThread");
            workHandlerThread.start();
            workHandler = new WorkHandler(coreParameters.senderQueueLength, new FLvMetaTagData(coreParameters), workHandlerThread
                    .getLooper());
        }
    }

    public void setConnectionListener(RESConnectionListener connectionListener) {
        synchronized (syncOp) {
            workHandler.setConnectionListener(connectionListener);
        }
    }

    @Override
    public String getServerIpAddr() {
        synchronized (syncOp) {
            return workHandler == null ? null : workHandler.getServerIpAddr();
        }
    }

    @Override
    public float getSendFrameRate() {
        synchronized (syncOp) {
            return workHandler == null ? 0 : workHandler.getSendFrameRate();
        }
    }

    @Override
    public float getSendBufferFreePercent() {
        synchronized (syncOp) {
            return workHandler == null ? 0 : workHandler.getSendBufferFreePercent();
        }
    }

    @Override
    public void start(String rtmpAddr) {
        synchronized (syncOp) {
            workHandler.start(rtmpAddr);
        }
    }

    @Override
    public void stop() {
        synchronized (syncOp) {
            workHandler.stop();
        }
    }

    @Override
    public void feed(RESFlvData flvData, int type) {
        synchronized (syncOp) {
            workHandler.feed(flvData, type);
        }
    }

    @Override
    public int getTotalSpeed() {
        synchronized (syncOp) {
            if (workHandler != null) {
                return workHandler.getTotalSpeed();
            }
            else {
                return 0;
            }
        }
    }

    //销毁
    public void destroy() {
        synchronized (syncOp) {
            workHandler.removeCallbacksAndMessages(null);
            workHandler.stop();
            if (Build.VERSION.SDK_INT > 18) {
                workHandlerThread.quitSafely();
            }
        }
    }

    //返回workHandler
    public WorkHandler getWorkHandler() {
        return workHandler;
    }

    public static class WorkHandler extends Handler implements IWorker {
        private final static int MSG_START = 1;
        private final static int MSG_WRITE = 2;
        private final static int MSG_STOP = 3;
        private long jniRtmpPointer = 0;
        private String serverIpAddr = null;
        private int maxQueueLength;
        private int writeMsgNum = 0;
        private final Object syncWriteMsgNum = new Object();

        private RESByteSpeedometer videoByteSpeedometer = new RESByteSpeedometer(TIMEGRANULARITY);
        private RESByteSpeedometer audioByteSpeedometer = new RESByteSpeedometer(TIMEGRANULARITY);
        private RESFrameRateMeter sendFrameRateMeter = new RESFrameRateMeter();
        private FLvMetaTagData fLvMetaTagData;
        private RESConnectionListener connectionListener;
        private final Object syncConnectionListener = new Object();
        private int errorTime = 0;

        private BufferFreeListener mListener = null;

        private enum STATE {
            IDLE, RUNNING, STOPPED
        }

        private STATE state;

        WorkHandler(int maxQueueLength, FLvMetaTagData fLvMetaTagData, Looper looper) {
            super(looper);
            this.maxQueueLength = maxQueueLength;
            this.fLvMetaTagData = fLvMetaTagData;
            state = STATE.IDLE;
        }

        @Override
        public void handleMessage(Message msg) {
            //todo: handle msg
            switch (msg.what) {
                case MSG_START:
                    if (state == STATE.RUNNING) {
                        break;
                    }
                    sendFrameRateMeter.reSet();
                    LogUtil.d("RESRtmpSender,WorkHandler,tid=" + Thread.currentThread().getId());
                    jniRtmpPointer = RtmpClient.open((String) msg.obj, true);
                    final int openR = jniRtmpPointer == 0 ? 1 : 0;
                    if (openR == 0) {
                        serverIpAddr = RtmpClient.getIpAddr(jniRtmpPointer);
                        LogUtil.d("serverIpAddr = " + serverIpAddr);
                    }
                    //使用主线程Handler调用回调
                    synchronized (syncConnectionListener) {
                        if (connectionListener != null) {
                            CallbackDelivery.getInstance().post(new Runnable() {
                                @Override
                                public void run() {
                                    connectionListener.onOpenConnectionResult(openR);
                                }
                            });
                        }
                    }
                    if (jniRtmpPointer == 0) {
                        break;
                    }
                    else {
                        byte[] metaData = fLvMetaTagData.getMetaData();
                        RtmpClient.write(jniRtmpPointer, metaData, metaData.length, RESFlvData.FLV_RTMP_PACKET_TYPE_INFO, 0);
                        state = STATE.RUNNING;
                    }
                    break;

                case MSG_STOP:
                    if (state == STATE.STOPPED || jniRtmpPointer == 0) {
                        break;
                    }
                    errorTime = 0;
                    final int closeR = RtmpClient.close(jniRtmpPointer);
                    serverIpAddr = null;
                    synchronized (syncConnectionListener) {
                        if (connectionListener != null) {
                            CallbackDelivery.getInstance().post(new Runnable() {
                                @Override
                                public void run() {
                                    connectionListener.onCloseConnectionResult(closeR);
                                }
                            });
                        }
                    }
                    state = STATE.STOPPED;
                    break;
                case MSG_WRITE:
                    synchronized (syncWriteMsgNum) {
                        --writeMsgNum;
                    }
                    if (state != STATE.RUNNING) {
                        break;
                    }
                    if (mListener != null) {
                        mListener.getBufferFree(getSendBufferFreePercent());
                    }
                    RESFlvData flvData = (RESFlvData) msg.obj;
                    if (writeMsgNum >= (maxQueueLength * 3 / 4) &&
                            flvData.flvTagType == RESFlvData.FLV_RTMP_PACKET_TYPE_VIDEO &&
                            flvData.droppable) {
                        LogUtil.d("senderQueue is crowded,abandon video");
                        break;
                    }
                    final int res = RtmpClient.write(jniRtmpPointer, flvData.byteBuffer, flvData.byteBuffer.length, flvData.flvTagType, flvData.dts);
                    if (res == 0) {
                        errorTime = 0;
                        if (flvData.flvTagType == RESFlvData.FLV_RTMP_PACKET_TYPE_VIDEO) {
                            videoByteSpeedometer.gain(flvData.size);
                            sendFrameRateMeter.count();
                        }
                        else {
                            audioByteSpeedometer.gain(flvData.size);
                        }
                    }
                    else {
                        ++errorTime;
                        synchronized (syncConnectionListener) {
                            if (connectionListener != null) {
                                CallbackDelivery.getInstance()
                                        .post(new RESConnectionListener.RESWriteErrorRunable(connectionListener, res));
                            }
                        }
                    }
                    break;
                default:
                    break;
            }


        }

        @Override
        public String getServerIpAddr() {
            return serverIpAddr;
        }

        @Override
        public float getSendFrameRate() {
            return sendFrameRateMeter.getFps();
        }

        @Override
        public float getSendBufferFreePercent() {
            synchronized (syncWriteMsgNum) {
                float res = (float) (maxQueueLength - writeMsgNum) / (float) maxQueueLength;
                return res <= 0 ? 0f : res;
            }
        }

        @Override
        public void start(String rtmpAddr) {
            this.removeMessages(MSG_START);
            synchronized (syncWriteMsgNum) {
                this.removeMessages(MSG_WRITE);
                writeMsgNum = 0;
            }
            this.sendMessage(this.obtainMessage(MSG_START, rtmpAddr));
        }

        @Override
        public void stop() {
            this.removeMessages(MSG_STOP);
            synchronized (syncWriteMsgNum) {
                this.removeMessages(MSG_WRITE);
                writeMsgNum = 0;
            }
            this.sendEmptyMessage(MSG_STOP);
        }

        @Override
        public void feed(RESFlvData flvData, int type) {
            synchronized (syncWriteMsgNum) {
                //LAKETODO optimize
                if (writeMsgNum <= maxQueueLength) {
                    this.sendMessage(this.obtainMessage(MSG_WRITE, type, 0, flvData));
                    ++writeMsgNum;
                }
                else {
                    LogUtil.d("senderQueue is full,abandon");
                }
            }
        }

        @Override
        public int getTotalSpeed() {
            return getVideoSpeed() + getAudioSpeed();
        }

        public int getVideoSpeed() {
            return videoByteSpeedometer.getSpeed();
        }

        public int getAudioSpeed() {
            return audioByteSpeedometer.getSpeed();
        }

        public void setConnectionListener(RESConnectionListener connectionListener) {
            synchronized (syncConnectionListener) {
                this.connectionListener = connectionListener;
            }
        }

        public interface BufferFreeListener {
            void getBufferFree(float free);
        }

        public void setBufferFreeListener(BufferFreeListener listener) {
            mListener = listener;
        }
    }
}
