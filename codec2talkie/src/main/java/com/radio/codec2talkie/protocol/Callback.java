package com.radio.codec2talkie.protocol;

import java.io.IOException;

public abstract class Callback {
    abstract protected void onSend(byte[] data) throws IOException;
    abstract protected void onReceive(byte [] frame);
}
