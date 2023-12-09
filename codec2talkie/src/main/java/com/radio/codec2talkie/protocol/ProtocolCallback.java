package com.radio.codec2talkie.protocol;

import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;

public abstract class ProtocolCallback {
    // receive
    abstract protected void onReceivePosition(Position position);
    abstract protected void onReceivePcmAudio(String src, String dst, short[] pcmFrame);
    abstract protected void onReceiveCompressedAudio(String src, String dst, byte[] frame);
    abstract protected void onReceiveTextMessage(TextMessage textMessage);
    abstract protected void onReceiveData(String src, String dst, String path, byte[] data);
    abstract protected void onReceiveSignalLevel(short rssi, short snr);
    abstract protected void onReceiveTelemetry(int batVoltage);
    abstract protected void onReceiveLog(String logData);

    // transmit
    abstract protected void onTransmitPcmAudio(String src, String dst, short[] frame);
    abstract protected void onTransmitCompressedAudio(String src, String dst, byte[] frame);
    abstract protected void onTransmitTextMessage(TextMessage textMessage);
    abstract protected void onTransmitPosition(Position position);
    abstract protected void onTransmitData(String src, String dst, String path, byte[] data);
    abstract protected void onTransmitLog(String logData);

    // errors
    abstract protected void onProtocolRxError();
    abstract protected void onProtocolTxError();
}