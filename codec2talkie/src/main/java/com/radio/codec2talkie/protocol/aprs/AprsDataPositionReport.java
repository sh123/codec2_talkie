package com.radio.codec2talkie.protocol.aprs;

import com.radio.codec2talkie.protocol.position.Position;

public class AprsDataPositionReport implements AprsData {

    @Override
    public void fromPosition(Position position) {
    }

    @Override
    public Position toPosition() {
        return null;
    }

    @Override
    public void fromBinary(byte[] infoData) {
    }

    @Override
    public byte[] toBinary() {
        return new byte[0];
    }

    @Override
    public boolean isValid() {
        return false;
    }
}
