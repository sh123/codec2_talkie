package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.util.Log;

import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;
import java.util.Arrays;

public class Raw implements Protocol {
    private static final String TAG = Raw.class.getSimpleName();

    private static final int RX_BUFFER_SIZE = 8192;

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
    public int getPcmAudioRecordBufferSize() {
        Log.w(TAG, "getPcmAudioBufferSize() is not supported");
        return -1;
    }

    @Override
    public void sendCompressedAudio(String src, String dst, byte[] frame) throws IOException {
        _transport.write(Arrays.copyOf(frame, frame.length));
    }

    @Override
    public void sendTextMessage(TextMessage textMessage) throws IOException {
        Log.w(TAG, "sendTextMessage() is not supported");
    }

    @Override
    public void sendPcmAudio(String src, String dst, short[] pcmFrame) {
        Log.w(TAG, "sendPcmAudio() is not supported");
    }

    @Override
    public void sendData(String src, String dst, String path, byte[] dataPacket) throws IOException {
        _transport.write(Arrays.copyOf(dataPacket, dataPacket.length));
    }

    @Override
    public boolean receive() throws IOException {
        int bytesRead = _transport.read(_rxDataBuffer);
        if (bytesRead > 0) {
            // NOTE, default data is compressed audio, upper layer should distinguish
            _parentProtocolCallback.onReceiveCompressedAudio(null, null, Arrays.copyOf(_rxDataBuffer, bytesRead));
            return true;
        }
        return false;
    }

    @Override
    public void sendPosition(Position position) {
        Log.w(TAG, "sendPosition() is not supported");
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
