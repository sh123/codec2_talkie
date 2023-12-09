package com.radio.opus;

import androidx.annotation.RequiresApi;

@RequiresApi(23)
public class Opus {

    static {
        System.loadLibrary("opus");
        System.loadLibrary("OpusJNI");
    }
    public native static long create(int sampleRate, int numChannels, int application, int bitrate, int complexity);
    public native static int destroy(long con);

    public native static int decode(long con, char[] in, short[] out, int frames);
    public native static int encode(long con, short[] in, int frames, char[] out);
}
