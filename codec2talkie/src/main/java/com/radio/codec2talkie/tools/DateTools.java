package com.radio.codec2talkie.tools;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateTools {
    public static String epochToIso8601(long timeMilliseconds) {
        String format = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date(timeMilliseconds));
    }

    public static String epochToIso8601Time(long timeMilliseconds) {
        String format = "HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date(timeMilliseconds));
    }

    public static long currentTimestampMinusHours(int hours) {
        return System.currentTimeMillis() - (hours * 60L * 60L * 1000L);
    }
}
