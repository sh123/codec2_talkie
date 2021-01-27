package com.radio.codec2talkie.tools;

public class AudioTools {

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
}
