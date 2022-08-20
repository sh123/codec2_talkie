package com.radio.codec2talkie.protocol.aprs;

import java.nio.ByteBuffer;

public class AprsDataFactory {
    public static AprsData create(AprsDataType aprsDataType) {
        switch (aprsDataType.getDataType()) {
            case UNKNOWN:
                break;
            case MIC_E:
                return new AprsDataPositionReportMicE();
            case POSITION_WITH_TIMESTAMP_MSG:
            case POSITION_WITH_TIMESTAMP_NO_MSG:
            case POSITION_WITHOUT_TIMESTAMP_NO_MSG:
            case POSITION_WITHOUT_TIMESTAMP_MSG:
                return new AprsDataPositionReport();
            case MESSAGE:
                return new AprsDataTextMessage();
        }
        return null;
    }

    public static AprsData fromBinary(String srcCallsign, String dstCallsign, String digipath, byte[] infoData) {
        ByteBuffer buffer = ByteBuffer.wrap(infoData);
        AprsDataType dataType = new AprsDataType((char)buffer.get());
        AprsData aprsData = create(dataType);
        if (aprsData == null) return null;
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        aprsData.fromBinary(srcCallsign, dstCallsign, digipath, data);
        return aprsData;
    }
}
