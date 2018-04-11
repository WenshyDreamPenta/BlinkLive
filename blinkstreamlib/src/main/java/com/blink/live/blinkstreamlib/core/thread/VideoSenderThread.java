package com.blink.live.blinkstreamlib.core.thread;

import android.media.MediaCodec;
import android.media.MediaFormat;

import com.blink.live.blinkstreamlib.core.PackagerCodec;
import com.blink.live.blinkstreamlib.rtmp.StreamFlvData;
import com.blink.live.blinkstreamlib.rtmp.StreamFlvDataCollecter;
import com.blink.live.blinkstreamlib.rtmp.RtmpPusher;
import com.blink.live.blinkstreamlib.utils.LogUtil;

import java.nio.ByteBuffer;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/13
 *     desc   : vedio 发送线程
 * </pre>
 */
public class VideoSenderThread extends Thread {
    private static final long WAIT_TIME = 5000;
    private MediaCodec.BufferInfo eInfo;
    private long startTime = 0;
    private MediaCodec dstVideoEncoder;
    private final Object syncDstVideoEncoder = new Object();
    private StreamFlvDataCollecter dataCollecter;
    private boolean shouldQuit = false;

    VideoSenderThread(String name, MediaCodec encoder, StreamFlvDataCollecter flvDataCollecter) {
        super(name);
        eInfo = new MediaCodec.BufferInfo();
        startTime = 0;
        dstVideoEncoder = encoder;
        dataCollecter = flvDataCollecter;
    }

    //更新编码器
    public void updateMediaCodec(MediaCodec encodec) {
        synchronized (syncDstVideoEncoder) {
            dstVideoEncoder = encodec;
        }
    }

    //退出线程
    public void quit() {
        shouldQuit = true;
        this.interrupt();
    }

    @Override
    public void run() {
        while (!shouldQuit) {
            synchronized (syncDstVideoEncoder) {
                int eobIndex = MediaCodec.INFO_TRY_AGAIN_LATER;
                try {
                    eobIndex = dstVideoEncoder.dequeueOutputBuffer(eInfo, WAIT_TIME);
                }
                catch (Exception exception) {
                }
                switch (eobIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        LogUtil.d("VideoSenderThread,MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        LogUtil.d("VideoSenderThread,MediaCodec.INFO_TRY_AGAIN_LATER");
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        LogUtil.d("VideoSenderThread,MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:" +
                                dstVideoEncoder.getOutputFormat().toString());
                        sendAVCDecoderConfigurationRecord(0, dstVideoEncoder.getOutputFormat());
                        break;
                    default:
                        LogUtil.d("VideoSenderThread,MediaCode,eobIndex=" + eobIndex);
                        if (startTime == 0) {
                            startTime = eInfo.presentationTimeUs / 1000;
                        }
                        /**
                         * we send sps pps already in INFO_OUTPUT_FORMAT_CHANGED
                         * so we ignore MediaCodec.BUFFER_FLAG_CODEC_CONFIG
                         */
                        if (eInfo.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG && eInfo.size != 0) {
                            ByteBuffer realData = dstVideoEncoder.getOutputBuffers()[eobIndex];
                            realData.position(eInfo.offset + 4);
                            realData.limit(eInfo.offset + eInfo.size);
                            sendRealData((eInfo.presentationTimeUs / 1000) - startTime, realData);
                        }
                        dstVideoEncoder.releaseOutputBuffer(eobIndex, false);
                        break;
                }

            }
            try {
                sleep(5);
            }
            catch (Exception e) {
            }
        }
        eInfo = null;
    }

    private void sendAVCDecoderConfigurationRecord(long tms, MediaFormat format) {
        byte[] AVCDecoderConfigurationRecord = PackagerCodec.H264Packager.generateAVCDecoderConfigurationRecord(format);
        int packetLen = PackagerCodec.FlvPackager.FLV_VIDEO_TAG_LENGTH + AVCDecoderConfigurationRecord.length;
        byte[] finalBuff = new byte[packetLen];
        PackagerCodec.FlvPackager.fillFlvVideoTag(finalBuff, 0, true, true, AVCDecoderConfigurationRecord.length);
        System.arraycopy(AVCDecoderConfigurationRecord, 0, finalBuff, PackagerCodec.FlvPackager.FLV_VIDEO_TAG_LENGTH, AVCDecoderConfigurationRecord.length);
        StreamFlvData streamFlvData = new StreamFlvData();
        streamFlvData.droppable = false;
        streamFlvData.byteBuffer = finalBuff;
        streamFlvData.size = finalBuff.length;
        streamFlvData.dts = (int) tms;
        streamFlvData.flvTagType = StreamFlvData.FLV_RTMP_PACKET_TYPE_VIDEO;
        streamFlvData.videoFrameType = StreamFlvData.NALU_TYPE_IDR;
        dataCollecter.collect(streamFlvData, RtmpPusher.FROM_VIDEO);
    }

    private void sendRealData(long tms, ByteBuffer realData) {
        int realDataLength = realData.remaining();
        int packetLen = PackagerCodec.FlvPackager.FLV_VIDEO_TAG_LENGTH + PackagerCodec.FlvPackager.NALU_HEADER_LENGTH + realDataLength;
        byte[] finalBuff = new byte[packetLen];
        realData.get(finalBuff, PackagerCodec.FlvPackager.FLV_VIDEO_TAG_LENGTH + PackagerCodec.FlvPackager.NALU_HEADER_LENGTH, realDataLength);
        int frameType = finalBuff[PackagerCodec.FlvPackager.FLV_VIDEO_TAG_LENGTH + PackagerCodec.FlvPackager.NALU_HEADER_LENGTH] & 0x1F;
        PackagerCodec.FlvPackager.fillFlvVideoTag(finalBuff, 0, false, frameType == 5, realDataLength);
        StreamFlvData streamFlvData = new StreamFlvData();
        streamFlvData.droppable = true;
        streamFlvData.byteBuffer = finalBuff;
        streamFlvData.size = finalBuff.length;
        streamFlvData.dts = (int) tms;
        streamFlvData.flvTagType = StreamFlvData.FLV_RTMP_PACKET_TYPE_VIDEO;
        streamFlvData.videoFrameType = frameType;
        dataCollecter.collect(streamFlvData, RtmpPusher.FROM_VIDEO);
    }
}
