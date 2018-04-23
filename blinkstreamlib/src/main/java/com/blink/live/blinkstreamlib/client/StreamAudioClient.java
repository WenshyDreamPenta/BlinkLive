package com.blink.live.blinkstreamlib.client;

import android.media.AudioRecord;

import com.blink.live.blinkstreamlib.core.audio.StreamAudioCore;
import com.blink.live.blinkstreamlib.model.StreamCoreParameters;

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
    private byte[] audioBuffer;
    private StreamAudioCore streamAudioCore;

    public StreamAudioClient(StreamCoreParameters streamCoreParameters){
        this.streamCoreParameters = streamCoreParameters;
    }
    
    class AudioRecordThread extends Thread{
        private boolean isRunning = false;
        AudioRecordThread(){
            isRunning = true;
        }
        @Override
        public void run() {
            while (isRunning){
                int size = audioRecord.read(audioBuffer, 0, audioBuffer.length);
            }
        }
    }
}
