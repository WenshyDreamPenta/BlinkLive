package com.blink.live.blinkstreamlib.core.audio;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import com.blink.live.blinkstreamlib.core.thread.AudioSenderThread;
import com.blink.live.blinkstreamlib.filter.BaseSoftAudioFilter;
import com.blink.live.blinkstreamlib.model.StreamAudioBuff;
import com.blink.live.blinkstreamlib.model.StreamConfig;
import com.blink.live.blinkstreamlib.model.StreamCoreParameters;
import com.blink.live.blinkstreamlib.rtmp.StreamFlvDataCollecter;
import com.blink.live.blinkstreamlib.tools.MediaCodecTools;
import com.blink.live.blinkstreamlib.utils.LogUtil;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/4/12
 *     desc   : Audio Stream核心服务
 * </pre>
 */
public class StreamAudioCore implements IAudioCore {
    StreamCoreParameters streamCoreParameters;
    private final Object SyncOp = new Object();
    private MediaCodec audioEncoder;
    private MediaFormat audioFormat;
    //filter
    private Lock lockAudioFilter = null;
    private BaseSoftAudioFilter audioFilter;
    private StreamAudioBuff[] orignAudioBuffs;
    private int lastAudioQueueBuffIndex;
    private StreamAudioBuff orignAudioBuff;
    private StreamAudioBuff filteredAudioBuff;
    private AudioCoreHandler audioCoreHandler;
    private HandlerThread audioHandlerThread;
    private AudioSenderThread audioSenderThread;


    public StreamAudioCore(StreamCoreParameters streamCoreParameters) {
        this.streamCoreParameters = streamCoreParameters;
        lockAudioFilter = new ReentrantLock(false);
    }
    @Override
    public void queueAudio(byte[] rawAudioFrame) {
        int targetIndex = (lastAudioQueueBuffIndex + 1) % orignAudioBuffs.length;
        if(orignAudioBuffs[targetIndex].isReadyToFill){
            System.arraycopy(rawAudioFrame, 0, orignAudioBuffs[targetIndex].buff, 0, streamCoreParameters.audioRecoderBufferSize);
            orignAudioBuffs[targetIndex].isReadyToFill = false;
            lastAudioQueueBuffIndex = targetIndex;
            audioCoreHandler.sendMessage(audioCoreHandler.obtainMessage(AudioCoreHandler.WHAT_INCOMING_BUFF, targetIndex, 0));
        }
    }

    @Override
    public boolean prepare(StreamConfig streamConfig) {
        synchronized (SyncOp){
            streamCoreParameters.mediacodecAACProfile = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
            streamCoreParameters.mediacodecAACSampleRate = 44100;
            streamCoreParameters.mediacodecAACChannelCount = 1;
            streamCoreParameters.mediacodecAACBitRate = 32 * 1024;
            streamCoreParameters.mediacodecAACMaxInputSize = 8820;

            audioFormat = new MediaFormat();
            audioEncoder = MediaCodecTools.createAudioMediaCodec(streamCoreParameters, audioFormat);
            if(audioEncoder == null){
                return false;
            }
            int audioQueueNum = streamCoreParameters.audioBufferQueueNum;
            int orignAudioBuffSize = streamCoreParameters.mediacodecAACSampleRate / 5;
            orignAudioBuffs = new StreamAudioBuff[audioQueueNum];
            for(int i = 0; i < audioQueueNum; i++){
                orignAudioBuffs[i] = new StreamAudioBuff(AudioFormat.ENCODING_PCM_16BIT, orignAudioBuffSize);
            }
            orignAudioBuff = new StreamAudioBuff(AudioFormat.ENCODING_PCM_16BIT, orignAudioBuffSize);
            filteredAudioBuff = new StreamAudioBuff(AudioFormat.ENCODING_PCM_16BIT, orignAudioBuffSize);
            return true;
        }
    }

    @Override
    public void start(StreamFlvDataCollecter streamFlvDataCollecter) {
        synchronized (SyncOp){
            try{
                for(StreamAudioBuff buff : orignAudioBuffs){
                    buff.isReadyToFill = true;
                }
                if(audioEncoder == null){
                    audioEncoder = MediaCodec.createEncoderByType(audioFormat.getString(MediaFormat.KEY_MIME));
                }
                audioEncoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                audioEncoder.start();
                lastAudioQueueBuffIndex = 0;
                audioHandlerThread = new HandlerThread("audioHandlerThread");
                audioHandlerThread.start();
                audioSenderThread.start();
                audioCoreHandler = new AudioCoreHandler(audioHandlerThread.getLooper());
            }catch (Exception e){
                LogUtil.trace(e);
            }
        }
    }

    @Override
    public void stop() {
        synchronized (SyncOp){
            audioCoreHandler.removeCallbacksAndMessages(null);
            audioHandlerThread.quit();
            try{
                audioHandlerThread.join();
                audioSenderThread.quit();
                audioSenderThread.join();

            }catch (Exception e){
                LogUtil.trace(e);
            }
            audioEncoder.stop();
            audioEncoder.release();
            audioEncoder = null;
        }
    }

    @Override
    public void destroy() {
        synchronized (SyncOp){
            lockAudioFilter.lock();
            if(audioFilter != null){
                audioFilter.onDestroy();
            }
            lockAudioFilter.unlock();
        }
    }

    public BaseSoftAudioFilter acquireAudioFilter(){
        lockAudioFilter.lock();
        return audioFilter;
    }

    public void releaseAudioFilter(){
        lockAudioFilter.unlock();
    }

    public void setAudioFilter(BaseSoftAudioFilter baseSoftAudioFilter){
        lockAudioFilter.lock();
        if(audioFilter != null){
            audioFilter.onDestroy();
        }
        audioFilter = baseSoftAudioFilter;
        if(audioFilter != null){
            audioFilter.onInit(streamCoreParameters.mediacodecAACSampleRate / 5);
        }
        lockAudioFilter.unlock();
    }

    private class AudioCoreHandler extends Handler {
        private static final int FILTER_LOCK_TOLERATION = 3;
        private static final int WHAT_INCOMING_BUFF = 1;
        private int sequenceNum;

        public AudioCoreHandler(Looper looper) {
            super(looper);
            this.sequenceNum = 0;
        }

        @Override
        public void handleMessage(Message msg) {
            if(msg.what != WHAT_INCOMING_BUFF){
                return;
            }
            sequenceNum ++;
            int targetIndex = msg.arg1;
            long nowTime = SystemClock.uptimeMillis();
            System.arraycopy(orignAudioBuffs[targetIndex].buff , 0 , orignAudioBuff.buff ,  0 , orignAudioBuff.buff.length);
            orignAudioBuffs[targetIndex].isReadyToFill = true;
            boolean isFilterLocked = lockAudioFilter();
            boolean filtered = false;
            if(isFilterLocked){
                filtered = audioFilter.onFrame(orignAudioBuff.buff, filteredAudioBuff.buff, nowTime, sequenceNum);
                unlockAudioFilter();
            }
            else {
                System.arraycopy(orignAudioBuffs[targetIndex].buff, 0, orignAudioBuff.buff, 0, orignAudioBuff.buff.length);
                orignAudioBuffs[targetIndex].isReadyToFill = true;
            }
            int eibIndex = audioEncoder.dequeueInputBuffer( -1);
            if(eibIndex >= 0){
                ByteBuffer dstAudioEncoderIBuffer = audioEncoder.getInputBuffers()[eibIndex];
                dstAudioEncoderIBuffer.position(0);
                dstAudioEncoderIBuffer.put(filtered?filteredAudioBuff.buff:orignAudioBuff.buff, 0, orignAudioBuff.buff.length);
                audioEncoder.queueInputBuffer(eibIndex, 0, orignAudioBuff.buff.length, nowTime * 1000, 0);
            }
        }

        /**
         * @return ture if filter locked & filter!=null
         */

        private boolean lockAudioFilter() {
            try {
                boolean locked = lockAudioFilter.tryLock(FILTER_LOCK_TOLERATION, TimeUnit.MILLISECONDS);
                if (locked) {
                    if (audioFilter != null) {
                        return true;
                    } else {
                        lockAudioFilter.unlock();
                        return false;
                    }
                } else {
                    return false;
                }
            } catch (InterruptedException e) {
            }
            return false;
        }

        private void unlockAudioFilter() {
            lockAudioFilter.unlock();
        }
    }
}
