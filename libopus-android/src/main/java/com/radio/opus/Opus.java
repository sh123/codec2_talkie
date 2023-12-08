package com.radio.opus;

import androidx.annotation.RequiresApi;

@RequiresApi(23)
public class Opus {

    static {
        System.loadLibrary("opus");
        System.loadLibrary("OpusJNI");
    }
}
