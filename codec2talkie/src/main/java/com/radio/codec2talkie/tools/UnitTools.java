package com.radio.codec2talkie.tools;

import java.util.Locale;

public class UnitTools {

    public static String decimalToNmea(double degrees, boolean isLatitude) {
        double degreesFractional = Math.abs(degrees) % 1;
        double degreesIntegral = Math.abs(degrees) - degreesFractional;
        degreesFractional *= 60;
        degreesIntegral *= 100;
        long nmeaDouble = (long)Math.round((degreesIntegral + degreesFractional) * 100.0);
        return String.format(
                Locale.US,
                isLatitude ? "%06d%c" : "%07d%c",
                nmeaDouble,
                isLatitude ? (degrees > 0 ? 'N' : 'S') : (degrees > 0 ? 'E' : 'W'));
    }

    public static double nmeaToDecimal(String degrees, String dir, boolean isLatitude) {
        return 0.0;
    }

    public static String decimalToDecimalNmea(double degrees, boolean isLatitude) {
        String value = decimalToNmea(degrees, isLatitude);
        return value.substring(0, isLatitude ? 4 : 5) +
                '.' +
                value.substring(isLatitude ? 4 : 5, value.length() - 1) +
                value.substring(value.length() - 1);
    }

    public static long metersToFeet(double meters) {
        return (long)(meters * 3.2808);
    }

    public static double feetToMeters(long feet) {
        return feet * 0.3048;
    }

    public static long metersPerSecondToKnots(float metersPerSecond) {
        return (long)(metersPerSecond / 0.514444);
    }

    public static float knotsToMetersPerSecond(long knots) {
        return (float) (knots * 0.5144444);
    }

    public static double metersPerSecondToMilesPerHour(float metersPerSecond) {
        return metersPerSecond * 2.23693629;
    }

    public static double kilometersPerSecondToMetersPerSecond(double kilometersPerHour) {
        return kilometersPerHour * 0.2777777778;
    }

    public static long minutesToMillis(long minutes ) {
        return minutes * 60L * 1000L;
    }

    public static long millisToSeconds(long milliseconds) { return milliseconds / 1000L; }
}
