package com.radio.codec2talkie.tools;

import java.util.Locale;

public class CoordinateTools {

    public static String decimalToNmea(double degrees, boolean isLatitude) {
        double degreesFractional = Math.abs(degrees) % 1;
        double degreesIntegral = Math.abs(degrees) - degreesFractional;
        degreesFractional *= 60;
        degreesIntegral *= 100;
        double nmeaDouble = Math.round((degreesIntegral + degreesFractional) * 100.0) / 100.0;
        return String.format(
                Locale.US,
                isLatitude ? "%.2f%c" : "0%.2f%c",
                nmeaDouble,
                isLatitude ? (degrees > 0 ? 'N' : 'S') : (degrees > 0 ? 'E' : 'W'));
    }
}
