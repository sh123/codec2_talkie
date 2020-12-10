package com.radio.codec2talkie.kiss;

import java.io.IOException;

public abstract class KissCallback {
    abstract protected void sendData(byte[] data) throws IOException;
    abstract protected void receiveFrame(byte [] frame);
}
