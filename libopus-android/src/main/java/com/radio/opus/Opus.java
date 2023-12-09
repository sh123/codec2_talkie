package com.radio.opus;

import androidx.annotation.RequiresApi;

@RequiresApi(23)
public class Opus {

    static {
        System.loadLibrary("opus");
        System.loadLibrary("OpusJNI");
    }

    // application
    public static final int OPUS_APPLICATION_VOIP = 2048;
    public static final int OPUS_APPLICATION_AUDIO = 2049;
    public static final int OPUS_APPLICATION_RESTRICTED_LOWDELAY = 2051;

    // frame sizes
    public static final int OPUS_FRAMESIZE_2_5_MS = 5001;   /**< Use 2.5 ms frames */
    public static final int OPUS_FRAMESIZE_5_MS = 5002;     /**< Use 5 ms frames */
    public static final int OPUS_FRAMESIZE_10_MS = 5003;    /**< Use 10 ms frames */
    public static final int OPUS_FRAMESIZE_20_MS = 5004;    /**< Use 20 ms frames */
    public static final int OPUS_FRAMESIZE_40_MS = 5005;    /**< Use 40 ms frames */
    public static final int OPUS_FRAMESIZE_60_MS = 5006;    /**< Use 60 ms frames */
    public static final int OPUS_FRAMESIZE_80_MS = 5007;    /**< Use 80 ms frames */
    public static final int OPUS_FRAMESIZE_100_MS = 5008;   /**< Use 100 ms frames */
    public static final int OPUS_FRAMESIZE_120_MS = 5009;   /**< Use 120 ms frames */

    // errors
    public static final int OPUS_OK = 0;
    public static final int OPUS_BAD_ARG = -1;
    public static final int OPUS_BUFFER_TOO_SMALL = -2;
    public static final int OPUS_INTERNAL_ERROR = -3;
    public static final int OPUS_INVALID_PACKET = -4;
    public static final int OPUS_UNIMPLEMENTED = -5;
    public static final int OPUS_INVALID_STATE = -6;
    public static final int OPUS_ALLOC_FAIL = -7;

    public native static long create(int sampleRate, int numChannels, int application, int bitrate, int complexity);
    public native static int destroy(long con);

    public native static int decode(long con, byte[] in, short[] out, int frames);
    public native static int encode(long con, short[] in, int frames, char[] out);
}
