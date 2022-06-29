package com.radio.codec2talkie.protocol;

import android.content.Context;

import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;
import java.util.Arrays;

public class Raw implements Protocol {

    private final int RX_BUFFER_SIZE = 8192;

    protected Transport _transport;
    protected final byte[] _rxDataBuffer;

    public Raw() {
        _rxDataBuffer = new byte[RX_BUFFER_SIZE];
    }

    @Override
    public void initialize(Transport transport, Context context) {
        _transport = transport;
    }

    @Override
    public int getPcmAudioBufferSize(int codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendCompressedAudio(String src, String dst, int codec2Mode, byte[] frame) throws IOException {
        _transport.write(Arrays.copyOf(frame, frame.length));
    }

    @Override
    public void sendPcmAudio(String src, String dst, int codec, short[] pcmFrame) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendData(String src, String dst, byte[] dataPacket) throws IOException {
        _transport.write(Arrays.copyOf(dataPacket, dataPacket.length));
    }

    @Override
    public boolean receive(Callback callback) throws IOException {
        int bytesRead = _transport.read(_rxDataBuffer);
        if (bytesRead > 0) {
            callback.onReceiveCompressedAudio(null, null, -1, Arrays.copyOf(_rxDataBuffer, bytesRead));
            return true;
        }
        return false;
    }

    @Override
    public void sendPosition(double latitude, double longitude, double altitude, float bearing, String comment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
