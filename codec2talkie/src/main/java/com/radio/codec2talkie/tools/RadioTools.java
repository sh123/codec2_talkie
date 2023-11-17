package com.radio.codec2talkie.tools;

import android.content.SharedPreferences;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.settings.SettingsWrapper;

public class RadioTools {
    
    public static final int ModulationTypeLora  = 0;
    public static final int ModulationTypeFsk = 1;

    public static int calculateLoraSpeedBps(int bw, int sf, int cr) {
        return (int)(sf * (4.0 / cr) / (Math.pow(2.0, sf) / bw));
    }

    public static int getRadioSpeed(SharedPreferences sharedPreferences) {
        int modulation = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_MOD, "0"));
        if (SettingsWrapper.isSoundModemEnabled(sharedPreferences)) {
            return SettingsWrapper.getFskSpeed(sharedPreferences);
        }
        if (modulation == ModulationTypeLora) {
            int resultBps = 0;
            int maxSpeedBps = 128000;
            try {
                if (!SettingsWrapper.isSoundModemEnabled(sharedPreferences) && SettingsWrapper.isKissExtensionEnabled(sharedPreferences)) {
                    int bw = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_BANDWIDTH, "125000"));
                    int sf = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_SF, "7"));
                    int cr = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_CR, "5"));
                    resultBps = RadioTools.calculateLoraSpeedBps(bw, sf, cr);
                }
            } catch (NumberFormatException | ArithmeticException e) {
                e.printStackTrace();
            }
            return (resultBps > 0 && resultBps <= maxSpeedBps) ? resultBps : 0;
        } else if (modulation == ModulationTypeFsk){
            return Integer.parseInt(sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_FSK_BIT_RATE, "4.8"));
        }
        return 0;
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

    public static int getMinimumDecodeSLevelLabel(SharedPreferences sharePreferences, int s0Level) {
        double sensitivityDbm = calculateLoraSensitivity(sharePreferences);
        int[] sLabels = {
                R.id.textViewRssi0,
                R.id.textViewRssi1,
                R.id.textViewRssi2,
                R.id.textViewRssi3,
                R.id.textViewRssi4,
                R.id.textViewRssi5,
                R.id.textViewRssi6,
                R.id.textViewRssi7,
                R.id.textViewRssi8,
                R.id.textViewRssi9,
                R.id.textViewRssi10,
                R.id.textViewRssi20,
                R.id.textViewRssi40
        };
        int index = (int) ((Math.abs(s0Level) - Math.abs(sensitivityDbm)) / 6.0);
        if (index < 0) {
            return sLabels[0];
        } else if (index >= sLabels.length) {
            return sLabels[sLabels.length-1];
        } else {
            return sLabels[index];
        }
    }
}

