package com.radio.codec2talkie.tools;

import android.content.SharedPreferences;
import android.graphics.Color;

import com.radio.codec2talkie.app.AppWorker;

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

    public static String getSpeedStatusText(String codec2ModeName, SharedPreferences sharedPreferences) {
        // codec2 speed
        String speedModeInfo = "C2: " + AudioTools.extractCodec2Speed(codec2ModeName);

        // radio speed
        int radioSpeedBps = RadioTools.getRadioSpeed(sharedPreferences);
        if (radioSpeedBps > 0) {
            speedModeInfo = "RF: " + radioSpeedBps + ", " + speedModeInfo;
        }
        return speedModeInfo;
    }

}
