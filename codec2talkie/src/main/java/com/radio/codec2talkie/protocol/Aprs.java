package com.radio.codec2talkie.protocol;

import android.content.Context;

import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;

public class Aprs implements Protocol {

    private final Protocol _childProtocol;

    public Aprs(Protocol childProtocol) {
        _childProtocol = childProtocol;
    }

    @Override
    public void initialize(Transport transport, Context context) throws IOException {
        _childProtocol.initialize(transport, context);
    }

    @Override
    public int getPcmAudioBufferSize(int codec) {
        return _childProtocol.getPcmAudioBufferSize(codec);
    }

    @Override
    public void sendCompressedAudio(String src, String dst, int codec2Mode, byte[] frame) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendPcmAudio(String src, String dst, int codec2Mode, short[] pcmFrame) throws IOException {
        // set src and dst if not provided
        _childProtocol.sendPcmAudio(src, dst, codec2Mode, pcmFrame);
    }

    @Override
    public void sendData(String src, String dst, byte[] dataPacket) throws IOException {
        _childProtocol.sendData(src, dst, dataPacket);
    }

    @Override
    public boolean receive(Callback callback) throws IOException {
        return _childProtocol.receive(new Callback() {
            @Override
            protected void onReceivePosition(double latitude, double longitude, double altitude, float bearing, String comment) {
                throw new UnsupportedOperationException();
            }

            @Override
            protected void onReceivePcmAudio(String src, String dst, int codec, short[] pcmFrame) {
                callback.onReceivePcmAudio(src, dst, codec, pcmFrame);
            }

            @Override
            protected void onReceiveCompressedAudio(String src, String dst, int codec2Mode, byte[] audioFrame) {
                throw new UnsupportedOperationException();
            }

            @Override
            protected void onReceiveData(String src, String dst, byte[] data) {
                // process aprs data and call onReceivePosition if position packet is received
                callback.onReceiveData(src, dst, data);
            }

            @Override
            protected void onReceiveSignalLevel(byte[] rawData) {
                callback.onReceiveSignalLevel(rawData);
            }

            @Override
            protected void onProtocolRxError() {
                callback.onProtocolRxError();
            }
        });
    }

    @Override
    public void sendPosition(double latitude, double longitude, double altitude, float bearing, String comment) {
        // TODO, implement
    }

    @Override
    public void flush() throws IOException {
        _childProtocol.flush();
    }

    @Override
    public void close() {
        _childProtocol.close();
    }
}
