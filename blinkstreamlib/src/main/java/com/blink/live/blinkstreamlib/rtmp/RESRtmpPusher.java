package com.blink.live.blinkstreamlib.rtmp;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.blink.live.blinkstreamlib.core.RESByteSpeedometer;
import com.blink.live.blinkstreamlib.core.RESFrameRateMeter;
import com.blink.live.blinkstreamlib.core.listeners.RESConnectionListener;
import com.blink.live.blinkstreamlib.model.RESCoreParameters;
import com.blink.live.blinkstreamlib.utils.LogTools;

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
    private Handler workerHandler;
    //同步-对象锁
    private final Object syncOp = new Object();

    //初始化参数
    public void preapare(RESCoreParameters coreParameters) {
        synchronized (syncOp) {
            workHandlerThread = new HandlerThread("RESRtmpSender,workHandlerThread");
            workHandlerThread.start();
            workerHandler = new WorkHandler(coreParameters.senderQueueLength, new FLvMetaTagData
                    (coreParameters), workHandlerThread.getLooper());
        }
    }

    @Override
    public String getServerIpAddr() {
        return null;
    }

    @Override
    public float getSendFrameRate() {
        return 0;
    }

    @Override
    public float getSendBufferFreePercent() {
        return 0;
    }

    @Override
    public void start(String rtmpAddr) {

    }

    @Override
    public void stop() {

    }

    @Override
    public void feed(RESFlvData flvData, int type) {

    }

    @Override
    public int getTotalSpeed() {
        return 0;
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
        public void handleMessage(Message msg){
            //todo: handle msg
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
                    LogTools.d("senderQueue is full,abandon");
                }
            }
        }

        @Override
        public int getTotalSpeed() {
            return 0;
        }

        public int getVideoSpeed() {
            return videoByteSpeedometer.getSpeed();
        }

        public int getAudioSpeed() {
            return audioByteSpeedometer.getSpeed();
        }

        public interface BufferFreeListener {
            void getBufferFree(float free);
        }

        public void setBufferFreeListener(BufferFreeListener listener) {
            mListener = listener;
        }
    }
}
