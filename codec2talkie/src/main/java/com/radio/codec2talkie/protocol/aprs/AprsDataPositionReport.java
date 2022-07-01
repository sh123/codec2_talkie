package com.radio.codec2talkie.protocol.aprs;

public class AprsDataPositionReport implements AprsData {
    @Override
    public void fromPosition(AprsPosition position) {
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
