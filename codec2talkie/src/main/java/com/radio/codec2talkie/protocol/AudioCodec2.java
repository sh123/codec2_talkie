package com.radio.codec2talkie.protocol;

import android.content.Context;

import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.transport.Transport;
import com.ustadmobile.codec2.Codec2;

import java.io.IOException;

public class AudioCodec2 implements Protocol {

    private final Protocol _childProtocol;

    private long _codec2Con;
    private int _audioBufferSize;

    private char[] _recordAudioEncodedBuffer;
    private short[] _playbackAudioBuffer;

    private ProtocolCallback _parentProtocolCallback;

    public AudioCodec2(Protocol childProtocol, int codec2ModeId) {
        _childProtocol = childProtocol;
        _codec2Con = 0;
        constructCodec2(codec2ModeId);
    }

    @Override
    public void initialize(Transport transport, Context context, ProtocolCallback protocolCallback) throws IOException {
        _parentProtocolCallback = protocolCallback;
        _childProtocol.initialize(transport, context, _protocolCallback);
    }

    @Override
    public int getPcmAudioRecordBufferSize() {
        return _audioBufferSize;
    }

    @Override
    public void sendCompressedAudio(String src, String dst, int codec2Mode, byte[] frame) throws IOException {
        _childProtocol.sendCompressedAudio(src, dst, codec2Mode, frame);
    }

    @Override
    public void sendTextMessage(TextMessage textMessage) throws IOException {
        _childProtocol.sendTextMessage(textMessage);
    }

    @Override
    public void sendPcmAudio(String src, String dst, int codec2Mode, short[] pcmFrame) throws IOException {
        _parentProtocolCallback.onTransmitPcmAudio(src, dst, codec2Mode, pcmFrame);

        Codec2.encode(_codec2Con, pcmFrame, _recordAudioEncodedBuffer);

        byte [] frame = new byte[_recordAudioEncodedBuffer.length];

        for (int i = 0; i < _recordAudioEncodedBuffer.length; i++) {
            frame[i] = (byte)_recordAudioEncodedBuffer[i];
        }
        _childProtocol.sendCompressedAudio(src, dst, codec2Mode, frame);
    }

    @Override
    public void sendData(String src, String dst, String path, byte[] dataPacket) throws IOException {
        _childProtocol.sendData(src, dst, path, dataPacket);
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
        protected void onReceivePcmAudio(String src, String dst, int codec, short[] pcmFrame) {
            _parentProtocolCallback.onReceivePcmAudio(src, dst, codec, pcmFrame);
        }

        @Override
        protected void onReceiveCompressedAudio(String src, String dst, int codec2Mode, byte[] audioFrame) {
            Codec2.decode(_codec2Con, _playbackAudioBuffer, audioFrame);
            _parentProtocolCallback.onReceivePcmAudio(src, dst, codec2Mode, _playbackAudioBuffer);
        }

        @Override
        protected void onReceiveTextMessage(TextMessage textMessage) {
            _parentProtocolCallback.onReceiveTextMessage(textMessage);
        }

        @Override
        protected void onReceiveData(String src, String dst, String path, byte[] data) {
            _parentProtocolCallback.onReceiveData(src, dst, path, data);
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
        protected void onTransmitPcmAudio(String src, String dst, int codec, short[] frame) {
            _parentProtocolCallback.onTransmitPcmAudio(src, dst, codec, frame);
        }

        @Override
        protected void onTransmitCompressedAudio(String src, String dst, int codec, byte[] frame) {
            _parentProtocolCallback.onTransmitCompressedAudio(src, dst, codec, frame);
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
        Codec2.destroy(_codec2Con);
        _childProtocol.close();
    }

    private void constructCodec2(int codecMode) {
        if (_codec2Con != 0) {
            Codec2.destroy(_codec2Con);
        }
        _codec2Con = Codec2.create(codecMode);

        _audioBufferSize = Codec2.getSamplesPerFrame(_codec2Con);
        int codec2FrameSize = Codec2.getBitsSize(_codec2Con); // returns number of bytes

        _recordAudioEncodedBuffer = new char[codec2FrameSize];
        _playbackAudioBuffer = new short[_audioBufferSize];
    }
}
