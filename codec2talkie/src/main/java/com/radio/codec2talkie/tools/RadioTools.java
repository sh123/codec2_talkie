package com.radio.codec2talkie.tools;

import android.content.SharedPreferences;

import com.radio.codec2talkie.settings.PreferenceKeys;

public class RadioTools {

    public static int calculateLoraSpeedBps(int bw, int sf, int cr) {
        return (int)(sf * (4.0 / cr) / (Math.pow(2.0, sf) / bw));
    }

    public static int getRadioSpeed(SharedPreferences sharedPreferences) {
        int resultBps = 0;
        int maxSpeedBps = 128000;
        try {
            if (sharedPreferences.getBoolean(PreferenceKeys.KISS_EXTENSIONS_ENABLED, false)) {
                int bw = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_BANDWIDTH, "125000"));
                int sf = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_SF, "7"));
                int cr = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_CR, "5"));
                resultBps = RadioTools.calculateLoraSpeedBps(bw, sf, cr);
            }
        } catch (NumberFormatException|ArithmeticException e) {
            e.printStackTrace();
        }
        return (resultBps > 0 && resultBps <= maxSpeedBps) ? resultBps : 0;
    }

    public static double calculateLoraSensitivity(SharedPreferences sharedPreferences) {
        int bw = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_BANDWIDTH, "125000"));
        int sf = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_SF, "5"));

        double snrLimit = -7.0;
        double noiseFigure = 6.0;
        switch (sf) {
            case 7:
                snrLimit = -7.5;
                break;
            case 8:
                snrLimit = -10.0;
                break;
            case 9:
                snrLimit = -12.6;
                break;
            case 10:
                snrLimit = -15.0;
                break;
            case 11:
                snrLimit = -17.5;
                break;
            case 12:
                snrLimit = -20.0;
                break;
        }
        return (-174 + 10 * Math.log10(bw) + noiseFigure + snrLimit);
    }
}

