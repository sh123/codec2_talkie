package com.radio.codec2talkie.tools;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.app.AppWorker;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.settings.SettingsWrapper;
import com.ustadmobile.codec2.Codec2;

public class AudioTools {

    private final static int UV_METER_MIN_DELTA = 30;
    private final static int UV_METER_MAX_DELTA = -20;

    public static int getSampleLevelDb(short [] pcmAudioSamples) {
        double db = -120.0;
        if (pcmAudioSamples != null) {
            double acc = 0;
            for (short v : pcmAudioSamples) {
                acc += Math.abs(v);
            }
            double avg = acc / pcmAudioSamples.length;
            db = (20.0 * Math.log10(avg / 32768.0));
        }
        return (int)db;
    }

    public static int colorFromAudioLevel(int audioLevel) {
        int color = Color.GREEN;
        if (audioLevel > AppWorker.getAudioMaxLevel() + UV_METER_MAX_DELTA)
            color = Color.RED;
        else if (audioLevel < AppWorker.getAudioMinLevel() + UV_METER_MIN_DELTA)
            color = Color.LTGRAY;
        return color;
    }

    public static int extractCodec2ModeId(String codec2ModeName) {
        String[] codecNameCodecId = codec2ModeName.split("=");
        return Integer.parseInt(codecNameCodecId[1]);
    }

    public static String extractCodec2Speed(String codec2ModeName) {
        String[] codecNameCodecId = codec2ModeName.split("=");
        String[] modeSpeed = codecNameCodecId[0].split("_");
        return modeSpeed[1];
    }

    public static String getFreedvModeAsText(SharedPreferences sharedPreferences) {
        if (SettingsWrapper.isFreeDvSoundModemModulation(sharedPreferences)) {
            switch (SettingsWrapper.getFreeDvSoundModemModulation(sharedPreferences)) {
                case Codec2.FREEDV_MODE_700C:
                    return "700C";
                case Codec2.FREEDV_MODE_700D:
                    return "700D";
                case Codec2.FREEDV_MODE_700E:
                    return "700E";
                case Codec2.FREEDV_MODE_1600:
                    return "1600";
                case Codec2.FREEDV_MODE_800XA:
                    return "800XA";
                case Codec2.FREEDV_MODE_2020:
                    return "2020";
                case Codec2.FREEDV_MODE_2020B:
                    return "2020B";
                case Codec2.FREEDV_MODE_2400A:
                    return "2400A";
                case Codec2.FREEDV_MODE_2400B:
                    return "2400B";
                default:
                    return null;
            }
        }
        return null;
    }

    public static String getModulationAsText(SharedPreferences sharedPreferences) {
        int modulation = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_MOD, "0"));
        return modulation == RadioTools.ModulationTypeLora ? "LoRa" : "FSK";
    }

    public static String getSpeedStatusText(SharedPreferences sharedPreferences, Resources resources) {

        // use freedv mode text instead if it is active
        String freedvModeLabel = getFreedvModeAsText(sharedPreferences);
        if (freedvModeLabel != null) return freedvModeLabel;

        // codec2 speed
        String speedModeInfo;
        if (SettingsWrapper.isCodec2Enabled(sharedPreferences)) {
            String codec2ModeName = sharedPreferences.getString(PreferenceKeys.CODEC2_MODE, resources.getStringArray(R.array.codec2_modes)[0]);
            speedModeInfo = "C2: " + AudioTools.extractCodec2Speed(codec2ModeName);
        } else {
            int speed = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.OPUS_BIT_RATE, "3200"));
            speedModeInfo = "OPUS: " + speed;
        }

        // radio speed
        int radioSpeedBps = RadioTools.getRadioSpeed(sharedPreferences);
        if (radioSpeedBps > 0) {
            speedModeInfo = "RF: " + radioSpeedBps + ", " + speedModeInfo;
        }
        return speedModeInfo;
    }

}
