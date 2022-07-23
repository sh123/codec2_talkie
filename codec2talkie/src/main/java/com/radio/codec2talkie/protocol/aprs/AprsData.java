package com.radio.codec2talkie.protocol.aprs;

import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;

public interface AprsData {
    void fromPosition(Position position);
    void fromTextMessage(TextMessage textMessage);
    Position toPosition();
    TextMessage toTextMessage();
    void fromBinary(String dstCallsign, byte[] infoData);
    byte[] toBinary();
    boolean isValid();
}
