package com.radio.codec2talkie.protocol.aprs.position;

public interface AprsPositionReport {
    void fromPosition(AprsPosition position);
    AprsPosition toPosition();
    void fromBinary(byte[] data);
    byte[] toBinary();
}
