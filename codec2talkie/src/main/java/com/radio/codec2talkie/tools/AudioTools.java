package com.radio.codec2talkie.tools;

import android.content.SharedPreferences;
import android.graphics.Color;

import com.radio.codec2talkie.audio.AudioProcessor;
import com.radio.codec2talkie.settings.PreferenceKeys;

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
        if (audioLevel > AudioProcessor.getAudioMaxLevel() + UV_METER_MAX_DELTA)
            color = Color.RED;
        else if (audioLevel < AudioProcessor.getAudioMinLevel() + UV_METER_MIN_DELTA)
            color = Color.LTGRAY;
        return color;
    }
}
