package com.radio.codec2talkie.protocol;

import android.content.Context;

import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;

public class Freedv implements Protocol {

    private final Protocol _childProtocol;
    private ProtocolCallback _parentProtocolCallback;

    public Freedv(Protocol childProtocol, int freedvMode) {
        _childProtocol = childProtocol;
    }

    @Override
    public void initialize(Transport transport, Context context, ProtocolCallback protocolCallback) throws IOException {
        _parentProtocolCallback = protocolCallback;
        _childProtocol.initialize(transport, context, _parentProtocolCallback);
    }

    @Override
    public int getPcmAudioBufferSize() {
        return 0;
    }

    @Override
    public void sendPcmAudio(String src, String dst, int codec, short[] pcmFrame) throws IOException {

    }

    @Override
    public void sendCompressedAudio(String src, String dst, int codec, byte[] frame) throws IOException {

    }

    @Override
    public void sendTextMessage(TextMessage textMessage) throws IOException {

    }

    @Override
    public void sendData(String src, String dst, byte[] dataPacket) throws IOException {

    }

    @Override
    public boolean receive() throws IOException {
        return false;
    }

    @Override
    public void sendPosition(Position position) throws IOException {

    }

    @Override
    public void flush() throws IOException {

    }

    @Override
    public void close() {

    }
}
