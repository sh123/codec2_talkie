package com.ustadmobile.codec2;

import androidx.annotation.RequiresApi;

@RequiresApi(23)
public class Codec2 {

    static {
        System.loadLibrary("codec2");
        System.loadLibrary("Codec2JNI");
    }

    public static final int CODEC2_MODE_3200 = 0;
    public static final int CODEC2_MODE_2400 = 1;
    public static final int CODEC2_MODE_1600 = 2;
    public static final int CODEC2_MODE_1400 = 3;
    public static final int CODEC2_MODE_1300 = 4;
    public static final int CODEC2_MODE_1200 = 5;
    public static final int CODEC2_MODE_700 = 6;
    public static final int CODEC2_MODE_700B = 7;
    public static final int CODEC2_MODE_700C = 8;
    public static final int CODEC2_MODE_WB = 9;
    public static final int CODEC2_MODE_450=10;
    public static final int CODEC2_MODE_450PWB=11;

    /**
     * The size of the file header that is written when using c2enc. When decoding, this must be
     * skipped.
     */
    public static final int CODEC2_FILE_HEADER_SIZE = 7;

    public native static long create(int mode);

    public native static int getSamplesPerFrame(long con);

    public native static int getBitsSize(long con);

    public native static int destroy(long con);

    public native static long encode(long con, short[] buf, char[] bits);

    /**
     * Decode one frame from codec2.
     *
     * @param con pointer long, as from the create method
     * @param outputBuffer buffer which will be filled with raw PCM audio decoded
     * @param bits input buffer containing one frame of audio
     *
     * @return 0 on successful completion
     */
    public native static long decode(long con, short[] outputBuffer, byte[] bits);

}