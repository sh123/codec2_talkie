package com.radio.codec2talkie.tools;

import java.util.Locale;

public class UnitTools {

    public static String decimalToNmea(double degrees, boolean isLatitude) {
        double degreesFractional = Math.abs(degrees) % 1;
        double degreesIntegral = Math.abs(degrees) - degreesFractional;
        degreesFractional *= 60;
        degreesIntegral *= 100;
        long nmeaDouble = Math.round((degreesIntegral + degreesFractional) * 100.0);
        return String.format(
                Locale.US,
                isLatitude ? "%06d%c" : "%07d%c",
                nmeaDouble,
                isLatitude ? (degrees > 0 ? 'N' : 'S') : (degrees > 0 ? 'E' : 'W'));
    }

    public static double nmeaToDecimal(String degrees, String dir) {
        // ddmm.mm / dddmm.mm
        int digitCount = degrees.charAt(4) == '.' ? 2 : 3;
        String integerPart = degrees.substring(0, digitCount);
        String fractionalPart = degrees.substring(digitCount);
        double v = Double.parseDouble(integerPart) + Double.parseDouble(fractionalPart) / 60.0;
        if (dir.equals("W") || dir.equals("S"))
            v = -v;
        return v;
    }

    public static String decimalToMaidenhead(double latitude, double longitude) {
        double lat = latitude + 90.0;
        double lon = longitude + 180.0;
        return String.format("%c%c%c%c%c%c",
                (char)('A' + (char)(lon / 20.0)),
                (char)('A' + (char)(lat / 10.0)),
                (char)('0' + (char)((lon % 20) / 2.0)),
                (char)('0' + (char)(lat % 10)),
                (char)('A' + (char)((lon - ((int)(lon / 2.0) * 2)) / (5.0 / 60.0))),
                (char)('A' + (char)((lat - ((int)(lat / 1.0) * 1)) / (2.5 / 60.0))));
    }

    public static String decimalToDecimalNmea(double degrees, boolean isLatitude) {
        String value = decimalToNmea(degrees, isLatitude);
        return value.substring(0, isLatitude ? 4 : 5) +
                '.' +
                value.substring(isLatitude ? 4 : 5, value.length() - 1) +
                value.substring(value.length() - 1);
    }

    public static long metersToFeet(double meters) {
        return (long)(meters * 3.28084);
    }

    public static double feetToMeters(long feet) {
        return feet / 3.28084;
    }

    public static long metersPerSecondToKnots(double metersPerSecond) {
        return (long)(metersPerSecond / 0.514444);
    }

    public static double knotsToMetersPerSecond(long knots) {
        return knots * 0.5144444;
    }

    public static double metersPerSecondToMilesPerHour(double metersPerSecond) {
        return metersPerSecond * 2.23693629;
    }

    public static double kilometersPerSecondToMetersPerSecond(double kilometersPerHour) {
        return kilometersPerHour * 0.2777777778;
    }

    public static long minutesToMillis(long minutes ) {
        return minutes * 60L * 1000L;
    }

    public static long millisToSeconds(long milliseconds) { return milliseconds / 1000L; }

    public static int metersPerSecondToKilometersPerHour(int speedMetersPerSecond) {
        return (int) (speedMetersPerSecond * 3.6);
    }
}
