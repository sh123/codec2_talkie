package com.radio.codec2talkie.protocol.aprs.position;

public class AprsPositionReportText implements AprsPositionReport {
    @Override
    public void fromPosition(AprsPosition position) {
    }

    @Override
    public AprsPosition toPosition() {
        return null;
    }

    @Override
    public void fromBinary(byte[] data) {
    }

    @Override
    public byte[] toBinary() {
        return new byte[0];
    }
}
