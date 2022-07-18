package com.radio.codec2talkie.protocol.aprs;

import java.nio.ByteBuffer;

public class AprsDataFactory {
    public static AprsData create(AprsDataType aprsDataType) {
        switch (aprsDataType.getDataType()) {
            case UNKNOWN:
            case POSITION_WITH_TIMESTAMP_MSG:
            case POSITION_WITH_TIMESTAMP_NO_MSG:
            case POSITION_WITHOUT_TIMESTAMP_NO_MSG:
                break;
            case MIC_E:
                return new AprsDataPositionReportMicE();
            case POSITION_WITHOUT_TIMESTAMP_MSG:
                return new AprsDataPositionReport();
            case MESSAGE:
                return new AprsDataTextMessage();
        }
        return null;
    }

    public static AprsData fromBinary(byte[] infoData) {
        ByteBuffer buffer = ByteBuffer.wrap(infoData);
        AprsDataType dataType = new AprsDataType(buffer.getChar());
        AprsData aprsData = create(dataType);
        if (aprsData == null) return null;
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        aprsData.fromBinary(data);
        return aprsData;
    }
}
