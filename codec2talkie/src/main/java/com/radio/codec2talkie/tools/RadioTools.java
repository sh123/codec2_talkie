package com.radio.codec2talkie.tools;

public class RadioTools {

    public static int calculateLoraSpeedBps(int bw, int sf, int cr) {
        return (int)(sf * (4.0 / cr) / (Math.pow(2.0, sf) / bw));
    }
}

