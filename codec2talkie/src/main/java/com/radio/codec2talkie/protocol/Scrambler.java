package com.radio.codec2talkie.protocol;

import android.content.Context;

import com.radio.codec2talkie.protocol.ciphers.ProtocolCipher;
import com.radio.codec2talkie.protocol.ciphers.ProtocolCipherFactory;
import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;

public class Scrambler implements Protocol {

    private static final String TAG = Scrambler.class.getSimpleName();

    private final Protocol _childProtocol;
    private ProtocolCallback _parentProtocolCallback;
    private ProtocolCipher _protocolCipher;

    public Scrambler(Protocol childProtocol) {
        _childProtocol = childProtocol;
    }

    @Override
    public void initialize(Transport transport, Context context, ProtocolCallback protocolCallback) throws IOException {
        _parentProtocolCallback = protocolCallback;
        _protocolCipher = ProtocolCipherFactory.create(context);
        _childProtocol.initialize(transport, context, _protocolCallback);
    }

    @Override
    public int getPcmAudioRecordBufferSize() {
        return _childProtocol.getPcmAudioRecordBufferSize();
    }

    @Override
    public void sendCompressedAudio(String src, String dst, byte[] audioFrame) throws IOException {
        byte[] result = scramble(audioFrame);
        if (result == null) {
            _parentProtocolCallback.onProtocolTxError();
        } else {
            _childProtocol.sendData(src, dst, null, result);
        }
    }

    @Override
    public void sendTextMessage(TextMessage textMessage) throws IOException {
        _childProtocol.sendTextMessage(textMessage);
    }

    @Override
    public void sendPcmAudio(String src, String dst, short[] pcmFrame) throws IOException {
        _childProtocol.sendPcmAudio(src, dst, pcmFrame);
    }

    @Override
    public void sendData(String src, String dst, String path, byte[] dataPacket) throws IOException {
        byte[] result = scramble(dataPacket);
        if (result == null) {
            _parentProtocolCallback.onProtocolTxError();
        } else {
            _childProtocol.sendData(src, dst, path, result);
        }
    }

    @Override
    public boolean receive() throws IOException {
        return _childProtocol.receive();
    }

    ProtocolCallback _protocolCallback = new ProtocolCallback() {
        @Override
        protected void onReceivePosition(Position position) {
            _parentProtocolCallback.onReceivePosition(position);
        }

        @Override
        protected void onReceivePcmAudio(String src, String dst, short[] pcmFrame) {
            _parentProtocolCallback.onReceivePcmAudio(src, dst, pcmFrame);
        }

        @Override
        protected void onReceiveCompressedAudio(String src, String dst, byte[] scrambledFrame) {

            byte[] audioFrames = unscramble(scrambledFrame);
            if (audioFrames == null) {
                _parentProtocolCallback.onProtocolRxError();
            } else {
                _parentProtocolCallback.onReceiveCompressedAudio(src, dst, audioFrames);
            }
        }

        @Override
        protected void onReceiveTextMessage(TextMessage textMessage) {
            _parentProtocolCallback.onReceiveTextMessage(textMessage);
        }

        @Override
        protected void onReceiveData(String src, String dst, String path, byte[] scrambledData) {
            byte[] data = unscramble(scrambledData);
            if (data == null) {
                _parentProtocolCallback.onProtocolRxError();
            } else {
                _parentProtocolCallback.onReceiveData(src, dst, path, data);
            }
        }

        @Override
        protected void onReceiveSignalLevel(short rssi, short snr) {
            _parentProtocolCallback.onReceiveSignalLevel(rssi, snr);
        }

        @Override
        protected void onReceiveTelemetry(int batVoltage) {
            _parentProtocolCallback.onReceiveTelemetry(batVoltage);
        }

        @Override
        protected void onReceiveLog(String logData) {
            _parentProtocolCallback.onReceiveLog(logData);
        }

        @Override
        protected void onTransmitPcmAudio(String src, String dst, short[] frame) {
            _parentProtocolCallback.onTransmitPcmAudio(src, dst, frame);
        }

        @Override
        protected void onTransmitCompressedAudio(String src, String dst, byte[] frame) {
            _parentProtocolCallback.onTransmitCompressedAudio(src, dst, frame);
        }

        @Override
        protected void onTransmitTextMessage(TextMessage textMessage) {
            _parentProtocolCallback.onTransmitTextMessage(textMessage);
        }

        @Override
        protected void onTransmitPosition(Position position) {
            _parentProtocolCallback.onTransmitPosition(position);
        }

        @Override
        protected void onTransmitData(String src, String dst, String path, byte[] data) {
            _parentProtocolCallback.onTransmitData(src, dst, path, data);
        }

        @Override
        protected void onTransmitLog(String logData) {
            _parentProtocolCallback.onTransmitLog(logData);
        }

        @Override
        protected void onProtocolRxError() {
            _parentProtocolCallback.onProtocolRxError();
        }

        @Override
        protected void onProtocolTxError() {
            _parentProtocolCallback.onProtocolTxError();
        }
    };

    @Override
    public void sendPosition(Position position) throws IOException {
        _childProtocol.sendPosition(position);
    }

    @Override
    public void flush() throws IOException {
        _childProtocol.flush();
    }

    @Override
    public void close() {
        _childProtocol.close();
    }

    private byte[] scramble(byte[] srcData) {
        return _protocolCipher.encrypt(srcData);
    }

    private byte[] unscramble(byte[] scrambledData) {
        return _protocolCipher.decrypt(scrambledData);
    }
}
