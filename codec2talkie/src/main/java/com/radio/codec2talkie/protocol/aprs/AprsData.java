package com.radio.codec2talkie.protocol.aprs;

import com.radio.codec2talkie.protocol.position.Position;

public interface AprsData {
    void fromPosition(Position position);
    Position toPosition();
    void fromBinary(byte[] infoData);
    byte[] toBinary();
    boolean isValid();
}
