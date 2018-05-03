package com.blink.live.blinkstreamlib.client;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.blink.live.blinkstreamlib.core.audio.StreamAudioCore;
import com.blink.live.blinkstreamlib.filter.BaseSoftAudioFilter;
import com.blink.live.blinkstreamlib.model.StreamConfig;
import com.blink.live.blinkstreamlib.model.StreamCoreParameters;
import com.blink.live.blinkstreamlib.rtmp.StreamFlvDataCollecter;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/4/12
 *     desc   : audio client
 * </pre>
 */
public class StreamAudioClient {
    private StreamCoreParameters streamCoreParameters;
    private final Object SynOp = new Object();
    private AudioRecord audioRecord;
    private AudioRecordThread audioRecordThread;
    private byte[] audioBuffer;
    private StreamAudioCore streamAudioCore;

    public StreamAudioClient(StreamCoreParameters streamCoreParameters) {
        this.streamCoreParameters = streamCoreParameters;
    }

    public boolean prepare(StreamConfig streamConfig) {
        synchronized (SynOp) {
            streamCoreParameters.audioBufferQueueNum = 5;
            streamAudioCore = new StreamAudioCore(streamCoreParameters);
            streamAudioCore.prepare(streamConfig);
            streamCoreParameters.audioRecoderFormat = AudioFormat.ENCODING_PCM_16BIT;
            streamCoreParameters.audioRecoderChannelConfig = AudioFormat.CHANNEL_IN_MONO;
            streamCoreParameters.audioRecoderSliceSize =
                    streamCoreParameters.mediacodecAACSampleRate / 10;
            streamCoreParameters.audioRecoderBufferSize =
                    streamCoreParameters.audioRecoderSliceSize * 2;
            streamCoreParameters.audioRecoderSource = MediaRecorder.AudioSource.DEFAULT;
            streamCoreParameters.audioRecoderSampleRate = streamCoreParameters.mediacodecAACSampleRate;
            prepareAudio();
            return true;
        }
    }

    public boolean start(StreamFlvDataCollecter streamFlvDataCollecter) {
        synchronized (SynOp) {
            streamAudioCore.start(streamFlvDataCollecter);
            audioRecord.startRecording();
            audioRecordThread = new AudioRecordThread();
            audioRecordThread.start();

            return true;
        }
    }

    public boolean stop() {
        synchronized (SynOp) {
            if (audioRecordThread != null) {
                audioRecordThread.quit();
                try {
                    audioRecordThread.join();
                }
                catch (InterruptedException ignored) {
                }
                streamAudioCore.stop();
                audioRecordThread = null;
                audioRecord.stop();
                return true;
            }
            return true;
        }
    }

    public boolean destroy() {
        synchronized (SynOp) {
            audioRecord.release();
            return true;
        }
    }

    public void setSoftAudioFilter(BaseSoftAudioFilter baseSoftAudioFilter) {
        streamAudioCore.setAudioFilter(baseSoftAudioFilter);
    }

    public BaseSoftAudioFilter acquireSoftAudioFilter() {
        return streamAudioCore.acquireAudioFilter();
    }

    public void releaseSoftAudioFilter() {
        streamAudioCore.releaseAudioFilter();
    }

    private boolean prepareAudio() {
        int minBufferSize = AudioRecord.getMinBufferSize(streamCoreParameters.audioRecoderSampleRate,
                streamCoreParameters.audioRecoderChannelConfig, streamCoreParameters.audioRecoderFormat);
        audioRecord = new AudioRecord(streamCoreParameters.audioRecoderSource, streamCoreParameters.audioRecoderSampleRate,
                streamCoreParameters.audioRecoderChannelConfig, streamCoreParameters.audioRecoderFormat, minBufferSize * 5);
        audioBuffer = new byte[streamCoreParameters.audioRecoderBufferSize];
        if (AudioRecord.STATE_INITIALIZED != audioRecord.getState()) {
            return false;
        }
        if (AudioRecord.SUCCESS !=
                audioRecord.setPositionNotificationPeriod(streamCoreParameters.audioRecoderSliceSize)) {
            return false;
        }
        return true;
    }

    class AudioRecordThread extends Thread {
        private boolean isRunning = false;

        AudioRecordThread() {
            isRunning = true;
        }

        public void quit() {
            isRunning = false;
        }

        @Override
        public void run() {
            while (isRunning) {
                int size = audioRecord.read(audioBuffer, 0, audioBuffer.length);
                if (isRunning && streamAudioCore != null && size > 0) {
                    streamAudioCore.queueAudio(audioBuffer);
                }
            }
        }
    }
}
