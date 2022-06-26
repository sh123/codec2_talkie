package com.radio.codec2talkie.protocol.ax25;

public class AX25Packet {

    public String src;
    public String dst;
    public String digipath;
    public int codec2Mode;
    public boolean isAudio;
    public byte[] rawData;
    public boolean isValid;

    public void fromBinary(byte[] data) {
        // TODO, implement
        src = null;
        dst = null;
        digipath = null;
        codec2Mode = -1;
        isAudio = true;
        rawData = data;
        isValid = true;
    }

    public byte[] toBinary() {
        // TODO, implement
        return rawData;
    }
}
