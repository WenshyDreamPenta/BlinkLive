package com.blink.live.blinkstreamlib.rtmp;

import com.blink.live.blinkstreamlib.model.StreamCoreParameters;

import java.util.ArrayList;


/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/11
 *     desc   :  This class is abel to generate a FLVTAG in accordance with Adobe Flash Video File
 *     Format Specification v10.1 Annex E.5 with limited types available.
 *     添加flV Tag 头格式
 * </pre>
 */
public class FLvMetaTagData {

    /**
     * FLV contains header and body
     * body contanis Tags and previous Tag size.
     * Tag contanis Header and Data.
     * Tag type: video/audio/script.
     * video type: 0x09; audio type: 0x08; script type: 0x12.
     * Audio Tag Data: 1Byte Audio Parameters: 4bit codecType、2bit sampling rate、1bit
     * precision、1bit type.
     * Video Tag Data: 1Byte Video Parameters: 4bit frame type、4bit codecType.
     * Script Tag Data：控制帧。会放一些关于FLV视频和音频的元数据信息如：duration、width、height等。
     * 通常该类型Tag会跟在File Header后面作为第一个Tag出现，而且只有一个。
     */
    private static final String Name = "onMetaData";
    private static final int ScriptData = 18; //flv script 类型
    private static final byte[] TS_SID = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static final byte[] ObjEndMarker = {0x00, 0x00, 0x09};
    private static final int EmptySize = 21;
    private ArrayList<byte[]> MetaData;//flv meta data
    private int DataSize;
    private int pointer;
    private byte[] MetaDataFrame;//flv 帧数据

    public FLvMetaTagData() {
        MetaData = new ArrayList<>();
        DataSize = 0;
    }

    //init audio video Parameters
    public FLvMetaTagData(StreamCoreParameters coreParameters) {
        this();
        //Audio code AAC
        setProperty("audiocodecid", 10);
        switch (coreParameters.mediacodecAACBitRate) {
            case 32 * 1024:
                setProperty("audiodatarate", 32);
                break;
            case 48 * 1024:
                setProperty("audiodatarate", 48);
                break;
            case 64 * 1024:
                setProperty("audiodatarate", 64);
                break;
        }

        switch (coreParameters.mediacodecAACSampleRate) {
            case 44100:
                setProperty("audiosamplerate", 44100);
                break;
            default:
                break;
        }
        //Video - h264(AVC)
        setProperty("videocodecid", 7);
        setProperty("framerate", coreParameters.mediacodecAVCFrameRate);
        setProperty("width", coreParameters.videoWidth);
        setProperty("height", coreParameters.videoHeight);
    }

    public void setProperty(String Key, int value) {
        addProperty(toFlvString(Key), (byte) 0, toFlvNum(value));
    }

    public void setProperty(String Key, String value) {
        addProperty(toFlvString(Key), (byte) 2, toFlvString(value));
    }

    //添加flv包体字段属性
    private void addProperty(byte[] Key, byte datatype, byte[] data) {
        int Propertysize = Key.length + 1 + data.length;
        byte[] Property = new byte[Propertysize];

        System.arraycopy(Key, 0, Property, 0, Key.length);
        Property[Key.length] = datatype;
        System.arraycopy(data, 0, Property, Key.length + 1, data.length);

        MetaData.add(Property);
        DataSize += Propertysize;
    }

    //获取flv元数据 字节数组
    public byte[] getMetaData() {
        MetaDataFrame = new byte[DataSize + EmptySize];
        pointer = 0;
        //SCRIPTDATA.name
        Addbyte(2);
        AddbyteArray(toFlvString(Name));
        //SCRIPTDATA.value ECMA array
        Addbyte(8);
        AddbyteArray(toUI(MetaData.size(), 4));
        for (byte[] Property : MetaData) {
            AddbyteArray(Property);
        }
        AddbyteArray(ObjEndMarker);

        return MetaDataFrame;
    }

    //添加数据
    private void Addbyte(int value) {
        MetaDataFrame[pointer] = (byte) value;
        pointer++;
    }

    //添加字节数组
    private void AddbyteArray(byte[] value) {
        System.arraycopy(value, 0, MetaDataFrame, pointer, value.length);
        pointer += value.length;
    }


    private byte[] toFlvString(String text) {
        byte[] FlvString = new byte[text.length() + 2];
        System.arraycopy(toUI(text.length(), 2), 0, FlvString, 0, 2);
        System.arraycopy(text.getBytes(), 0, FlvString, 2, text.length());
        return FlvString;
    }

    private byte[] toUI(long value, int bytes) {
        byte[] UI = new byte[bytes];
        for (int i = 0; i < bytes; i++) {
            UI[bytes - 1 - i] = (byte) (value >> (8 * i) & 0xff);
        }
        return UI;
    }

    private byte[] toFlvNum(double value) {
        long tmp = Double.doubleToLongBits(value);
        return toUI(tmp, 8);
    }
}
