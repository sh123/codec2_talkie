package com.radio.codec2talkie.protocol.aprs;

import java.nio.ByteBuffer;

public class AprsObject extends AprsDataPositionReport {
    private static final String TAG = AprsObject.class.getSimpleName();

    @Override
    public void fromBinary(String srcCallsign, String dstCallsign, String digipath, byte[] infoData) {
        if (infoData.length <= 9) return;
        ByteBuffer buffer = ByteBuffer.wrap(infoData);

        // callsign
        byte[] objectCallsign = new byte[9];
        buffer.get(objectCallsign, 0, 9);

        // process only live objects
        byte isLive = buffer.get();
        if (isLive != '*') return;

        byte[] positionInfoData = new byte[buffer.remaining()];
        buffer.get(positionInfoData);

        String positionSrcCallsign = new String(objectCallsign).trim();

        super.fromBinary(positionSrcCallsign, dstCallsign, digipath, positionInfoData);
    }
}
