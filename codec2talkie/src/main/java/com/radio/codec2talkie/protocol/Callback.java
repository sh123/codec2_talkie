package com.radio.codec2talkie.protocol;

public abstract class Callback {
    abstract protected void onReceiveAudioFrames(byte[] frame);
    abstract protected void onReceiveSignalLevel(byte[] rawData);
    abstract protected void onProtocolRxError();
}
