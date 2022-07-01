package com.radio.codec2talkie.protocol.aprs.position;

import com.radio.codec2talkie.protocol.aprs.AprsDataType;

public class AprsPositionReportFactory {
    public static AprsPositionReport create(AprsDataType aprsDataType) {
        switch (aprsDataType.getDataType()) {
            case UNKNOWN:
            case MESSAGE:
                break;
            case MIC_E:
                return new AprsPositionReportMicE();
            case POSITION_WITH_TIMESTAMP_MSG:
            case POSITION_WITH_TIMESTAMP_NO_MSG:
                return new AprsPositionReportTextWithTimestamp();
            case POSITION_WITHOUT_TIMESTAMP_MSG:
            case POSITION_WITHOUT_TIMESTAMP_NO_MSG:
                return new AprsPositionReportText();
        }
        return null;
    }
}
