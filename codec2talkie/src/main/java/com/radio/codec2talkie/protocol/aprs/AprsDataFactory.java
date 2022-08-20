package com.radio.codec2talkie.protocol.aprs;

import java.nio.ByteBuffer;
import java.util.Arrays;

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

    public static AprsData fromAprsIs(String textData) {
        String[] callsignData = textData.split(">");
        if (callsignData.length < 2) return null;
        String srcCallsign = callsignData[0];
        String[] digipathData = callsignData[1].split(":");
        if (digipathData.length < 2) return null;
        String[] path = digipathData[0].split(",");
        if (path.length == 0) return null;
        String dstCallsign = path[0];
        String data = digipathData[1];
        String[] filteredPath = new String[path.length - 1];
        System.arraycopy(path, 1, filteredPath, 0, path.length - 1);
        StringBuilder digipath = new StringBuilder();
        String sep = "";
        for (String p : filteredPath) {
            if (p.startsWith("WIDE")) {
                digipath.append(sep);
                digipath.append(p);
                sep = ",";
            }
        }
        return fromBinary(srcCallsign, dstCallsign, digipath.toString(), data.getBytes());
    }
}
