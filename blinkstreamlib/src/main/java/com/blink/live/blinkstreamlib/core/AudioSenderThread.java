package com.blink.live.blinkstreamlib.core;

import android.media.MediaCodec;

import com.blink.live.blinkstreamlib.rtmp.RESFlvData;
import com.blink.live.blinkstreamlib.rtmp.RESFlvDataCollecter;
import com.blink.live.blinkstreamlib.rtmp.RESRtmpPusher;
import com.blink.live.blinkstreamlib.utils.LogTools;

import java.nio.ByteBuffer;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/15
 *     desc   : audio 发送线程
 * </pre>
 */
public class AudioSenderThread extends Thread {
    //ms
    private static final long WAIT_TIME = 5000;
    private MediaCodec.BufferInfo mBufferInfo;
    private long startTime = 0;
    private MediaCodec audioEncoder;
    private RESFlvDataCollecter dataCollecter;

    private boolean shouldQuit = false;

    AudioSenderThread(String name, MediaCodec encoder, RESFlvDataCollecter flvDataCollecter) {
        super(name);
        mBufferInfo = new MediaCodec.BufferInfo();
        startTime = 0;
        audioEncoder = encoder;
        dataCollecter = flvDataCollecter;
    }

    //中断线程
    void quit() {
        shouldQuit = true;
        this.interrupt();
    }

    @Override
    public void run() {
        super.run();
        while (!shouldQuit) {
            int eobIndex = audioEncoder.dequeueOutputBuffer(mBufferInfo, WAIT_TIME);
            switch (eobIndex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    LogTools.d("AudioSenderThread,MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    LogTools.d("AudioSenderThread,MediaCodec.INFO_TRY_AGAIN_LATER");
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    LogTools.d("AudioSenderThread,MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:" + audioEncoder.getOutputFormat().toString());
                    ByteBuffer csd0 = audioEncoder.getOutputFormat().getByteBuffer("csd-0");
                    sendAudioSpecificConfig(0, csd0);
                    break;
                default:
                    LogTools.d("AudioSenderThread,MediaCode,eobIndex=" + eobIndex);
                    if (startTime == 0) {
                        startTime = mBufferInfo.presentationTimeUs / 1000;
                    }
                    /**
                     * we send audio SpecificConfig already in INFO_OUTPUT_FORMAT_CHANGED
                     * so we ignore MediaCodec.BUFFER_FLAG_CODEC_CONFIG
                     */
                    if (mBufferInfo.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG &&
                            mBufferInfo.size != 0) {
                        ByteBuffer realData = audioEncoder.getOutputBuffers()[eobIndex];
                        realData.position(mBufferInfo.offset);
                        realData.limit(mBufferInfo.offset + mBufferInfo.size);
                        sendRealData((mBufferInfo.presentationTimeUs / 1000) - startTime, realData);
                    }
                    audioEncoder.releaseOutputBuffer(eobIndex, false);
                    break;
            }
        }
        mBufferInfo = null;
    }

    //发送audio配置
    private void sendAudioSpecificConfig(long tms, ByteBuffer realData) {
        int packetLen = PackagerCodec.FlvPackager.FLV_AUDIO_TAG_LENGTH + realData.remaining();
        byte[] finalBuff = new byte[packetLen];
        realData.get(finalBuff, PackagerCodec.FlvPackager.FLV_AUDIO_TAG_LENGTH, realData.remaining());
        PackagerCodec.FlvPackager.fillFlvAudioTag(finalBuff, 0, true);
        RESFlvData resFlvData = new RESFlvData();
        resFlvData.droppable = false;
        resFlvData.byteBuffer = finalBuff;
        resFlvData.size = finalBuff.length;
        resFlvData.dts = (int) tms;
        resFlvData.flvTagType = RESFlvData.FLV_RTMP_PACKET_TYPE_AUDIO;
        dataCollecter.collect(resFlvData, RESRtmpPusher.FROM_AUDIO);
    }

    //发送audio 数据
    private void sendRealData(long tms, ByteBuffer realData) {
        int packetLen = PackagerCodec.FlvPackager.FLV_AUDIO_TAG_LENGTH + realData.remaining();
        byte[] finalBuff = new byte[packetLen];
        realData.get(finalBuff, PackagerCodec.FlvPackager.FLV_AUDIO_TAG_LENGTH, realData.remaining());
        PackagerCodec.FlvPackager.fillFlvAudioTag(finalBuff, 0, false);
        RESFlvData resFlvData = new RESFlvData();
        resFlvData.droppable = true;
        resFlvData.byteBuffer = finalBuff;
        resFlvData.size = finalBuff.length;
        resFlvData.dts = (int) tms;
        resFlvData.flvTagType = RESFlvData.FLV_RTMP_PACKET_TYPE_AUDIO;
        dataCollecter.collect(resFlvData, RESRtmpPusher.FROM_AUDIO);
    }

}
