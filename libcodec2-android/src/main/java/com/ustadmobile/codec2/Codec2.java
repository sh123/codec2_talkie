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
    public static final int CODEC2_MODE_700C = 8;
    public static final int CODEC2_MODE_450=10;
    public static final int CODEC2_MODE_450PWB=11;

    /**
     * The size of the file header that is written when using c2enc. When decoding, this must be
     * skipped.
     */
    public static final int CODEC2_FILE_HEADER_SIZE = 7;

    public native static long create(int mode);
    public native static int destroy(long con);

    public native static int getSamplesPerFrame(long con);
    public native static int getBitsSize(long con);

    public native static long encode(long con, short[] inputSamples, char[] outputBits);
    public native static long decode(long con, short[] outputSamples, byte[] inputsBits);

    public native static long fskCreate(int sampleFrequency, int symbolRate, int toneFreq, int toneSpacing, int gain);
    public native static int fskDestroy(long conFsk);

    public native static int fskDemodBitsBufSize(long conFsk);
    public native static int fskModSamplesBufSize(long conFsk);
    public native static int fskDemodSamplesBufSize(long conFsk);
    public native static int fskModBitsBufSize(long conFsk);
    public native static int fskSamplesPerSymbol(long conFsk);
    public native static int fskNin(long conFsk);

    public native static long fskModulate(long conFsk, short[] outputSamples, byte[] inputBits);
    public native static long fskDemodulate(long conFsk, short[] inputSamples, byte[] outputBits);
}