package com.radio.codec2talkie.protocol;

import android.content.Context;

import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;
import java.util.Arrays;

public class Raw implements Protocol {

    private final int RX_BUFFER_SIZE = 8192;

    protected Transport _transport;
    protected final byte[] _rxDataBuffer;

    private ProtocolCallback _parentProtocolCallback;

    public Raw() {
        _rxDataBuffer = new byte[RX_BUFFER_SIZE];
    }

    @Override
    public void initialize(Transport transport, Context context, ProtocolCallback protocolCallback) {
        _transport = transport;
        _parentProtocolCallback = protocolCallback;
    }

    @Override
    public int getPcmAudioBufferSize() {
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
    public boolean receive() throws IOException {
        int bytesRead = _transport.read(_rxDataBuffer);
        if (bytesRead > 0) {
            _parentProtocolCallback.onReceiveCompressedAudio(null, null, -1, Arrays.copyOf(_rxDataBuffer, bytesRead));
            return true;
        }
        return false;
    }

    @Override
    public void sendPosition(Position position) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
