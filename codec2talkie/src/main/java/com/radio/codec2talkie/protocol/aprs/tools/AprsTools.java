package com.radio.codec2talkie.protocol.aprs.tools;

public class AprsTools {
    public static String applyPrivacyOnUncompressedNmeaCoordinate(String nmeaCoordinate, int privacyLevel) {
        byte [] buffer = nmeaCoordinate.getBytes();
        int level = 0;
        for (int i = buffer.length - 2; i > 0 && level < privacyLevel; i--) {
            if (buffer[i] == '.') continue;
            buffer[i] = ' ';
            level++;
        }
        return new String(buffer);
    }
}
