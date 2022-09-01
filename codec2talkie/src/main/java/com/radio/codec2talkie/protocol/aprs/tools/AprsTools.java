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

    public static int phgToDirectivityDegrees(String phg) {
        int d = phg.charAt(6) - '0';
        if (d > 8) return 0;
        return 45*d;
    }

    public static double phgToRangeMiles(String phg) {
        int p = phg.charAt(3) - '0';
        double power = p*p;
        int h = phg.charAt(4) - '0';
        double haat = 10.0 * Math.pow(2.0, h);
        int g = phg.charAt(5) - '0';
        double gain = Math.pow(10.0, g / 10.0);
        return Math.sqrt(2.0 * haat * Math.sqrt((power / 10.0) * (gain / 2.0)));
    }
}
