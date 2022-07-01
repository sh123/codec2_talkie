package com.radio.codec2talkie.protocol.aprs;

import com.radio.codec2talkie.tools.DebugTools;

import java.nio.ByteBuffer;

public class AprsData {
    public boolean isValid;

    public String srcCallsign;
    public String dstCallsign;
    public String digipath;

    public AprsDataType aprsDataType;

    private String _infoString;

    public AprsData(String src, String dst, String path) {
        isValid = false;
        srcCallsign = src;
        dstCallsign = dst;
        digipath = path;
    }

    public void fromBinary(byte[] rawPacket) {
        _infoString = DebugTools.bytesToDebugString(rawPacket);
        ByteBuffer buffer = ByteBuffer.wrap(rawPacket);
        char dataTypeIdent = buffer.getChar();
        aprsDataType = new AprsDataType(dataTypeIdent);
    }

    public String toString() {
        return String.format("%s>%s,%s:%s", srcCallsign, dstCallsign, digipath, _infoString);
    }
}
