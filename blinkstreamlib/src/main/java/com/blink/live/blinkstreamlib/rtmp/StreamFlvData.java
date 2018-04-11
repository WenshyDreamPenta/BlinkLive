package com.blink.live.blinkstreamlib.rtmp;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/12
 *     desc   : RESFlv数据封装
 * </pre>
 */
public class StreamFlvData {

    public final static int FLV_RTMP_PACKET_TYPE_VIDEO = 9;
    public final static int FLV_RTMP_PACKET_TYPE_AUDIO = 8;
    public final static int FLV_RTMP_PACKET_TYPE_INFO = 18;
    public final static int NALU_TYPE_IDR = 5;

    public boolean droppable;

    public int dts;//解码时间戳

    public byte[] byteBuffer; //数据

    public int size; //字节长度

    public int flvTagType; //视频和音频的分类

    public int videoFrameType;

    //是否关键帧
    public boolean isKeyframe() {
        return videoFrameType == NALU_TYPE_IDR;
    }

}
