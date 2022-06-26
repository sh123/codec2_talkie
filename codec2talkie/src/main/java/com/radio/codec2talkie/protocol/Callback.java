package com.radio.codec2talkie.protocol;

public abstract class Callback {
    abstract protected void onReceiveAudioFrames(String src, String dst, int codec, byte[] frame);
    abstract protected void onReceiveData(String src, String dst, byte[] data);
    abstract protected void onReceiveSignalLevel(byte[] rawData);
    abstract protected void onProtocolRxError();
}
