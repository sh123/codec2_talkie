package com.radio.codec2talkie.protocol;

import android.content.Context;

import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.transport.Transport;
import com.ustadmobile.codec2.Codec2;

import java.io.IOException;
import java.util.Arrays;

public class Freedv implements Protocol {
    private final int _freedvMode;
    private final Protocol _childProtocol;

    private ProtocolCallback _parentProtocolCallback;
    private Transport _transport;

    private long _freedv;

    private short[] _modemTxBuffer;
    private short[] _speechRxBuffer;
    private short[] _modemRxBuffer;

    public Freedv(Protocol childProtocol, int freedvMode) {
        _childProtocol = childProtocol;
        _freedvMode = freedvMode;
    }

    @Override
    public void initialize(Transport transport, Context context, ProtocolCallback parentProtocolCallback) throws IOException {
        _transport = transport;
        _parentProtocolCallback = parentProtocolCallback;
        _childProtocol.initialize(transport, context, _parentProtocolCallback);

        _freedv = Codec2.freedvCreate(_freedvMode);
        _modemTxBuffer = new short[Codec2.freedvGetNomModemSamples(_freedv)];
        _modemRxBuffer = new short[Codec2.freedvGetMaxModemSamples(_freedv)];
        _speechRxBuffer = new short[Codec2.freedvGetMaxSpeechSamples(_freedv)];
    }

    @Override
    public int getPcmAudioBufferSize() {
        return Codec2.freedvGetNSpeechSamples(_freedv);
    }

    @Override
    public void sendPcmAudio(String src, String dst, int codec, short[] pcmFrame) throws IOException {
        Codec2.freedvTx(_freedv, _modemTxBuffer, pcmFrame);
        // _transport.write(_modemTxBuffer);
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
        // int bytesRead = _transport.read(_modemRxBuffer);
        long cntRead = Codec2.freedvRx(_freedv, _speechRxBuffer, _modemRxBuffer);
        if (cntRead > 0) {
            _parentProtocolCallback.onReceivePcmAudio(null, null, -1, Arrays.copyOf(_modemRxBuffer, (int) cntRead));
        }
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
        Codec2.freedvDestroy(_freedv);
    }
}
