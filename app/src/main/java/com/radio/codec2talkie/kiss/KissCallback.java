package com.radio.codec2talkie.kiss;

public abstract class KissCallback {
    abstract protected void sendByte(byte b);
    abstract protected void receiveFrame(byte [] frame);
}
