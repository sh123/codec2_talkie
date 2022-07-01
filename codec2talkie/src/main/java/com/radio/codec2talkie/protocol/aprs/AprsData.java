package com.radio.codec2talkie.protocol.aprs;

public interface AprsData {
    void fromPosition(AprsPosition position);
    void fromBinary(byte[] infoData);
    byte[] toBinary();
    boolean isValid();
}
