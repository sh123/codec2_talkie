package com.radio.codec2talkie.protocol;

public abstract class Callback {
    // receive
    abstract protected void onReceivePosition(String src, double latitude, double longitude, double altitude, float bearing, String comment);
    abstract protected void onReceivePcmAudio(String src, String dst, int codec, short[] pcmFrame);
    abstract protected void onReceiveCompressedAudio(String src, String dst, int codec, byte[] frame);
    abstract protected void onReceiveData(String src, String dst, byte[] data);
    abstract protected void onReceiveSignalLevel(short rssi, short snr);
    abstract protected void onReceiveLog(String logData);

    // transmit
    abstract protected void onTransmitPcmAudio(String src, String dst, int codec, short[] frame);
    abstract protected void onTransmitCompressedAudio(String src, String dst, int codec, byte[] frame);
    abstract protected void onTransmitData(String src, String dst, byte[] data);
    abstract protected void onTransmitLog(String logData);

    // errors
    abstract protected void onProtocolRxError();
    abstract protected void onProtocolTxError();
}