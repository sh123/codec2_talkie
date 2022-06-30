package com.radio.codec2talkie.protocol;

import android.content.Context;

import com.radio.codec2talkie.transport.Transport;
import com.ustadmobile.codec2.Codec2;

import java.io.IOException;

public class AudioCodec2 implements Protocol {

    private final Protocol _childProtocol;

    private long _codec2Con;
    private int _codec2Mode;
    private int _audioBufferSize;

    private char[] _recordAudioEncodedBuffer;
    private short[] _playbackAudioBuffer;

    public AudioCodec2(Protocol childProtocol, int codec2ModeId) {
        _childProtocol = childProtocol;
        _codec2Con = 0;
        constructCodec2(codec2ModeId);
    }

    @Override
    public void initialize(Transport transport, Context context) throws IOException {
        _childProtocol.initialize(transport, context);
    }

    @Override
    public int getPcmAudioBufferSize() {
        return _audioBufferSize;
    }

    @Override
    public void sendCompressedAudio(String src, String dst, int codec2Mode, byte[] frame) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendPcmAudio(String src, String dst, int codec2Mode, short[] pcmFrame) throws IOException {
        Codec2.encode(_codec2Con, pcmFrame, _recordAudioEncodedBuffer);

        byte [] frame = new byte[_recordAudioEncodedBuffer.length];

        for (int i = 0; i < _recordAudioEncodedBuffer.length; i++) {
            frame[i] = (byte)_recordAudioEncodedBuffer[i];
        }
        _childProtocol.sendCompressedAudio(src, dst, codec2Mode, frame);
    }

    @Override
    public void sendData(String src, String dst, byte[] dataPacket) throws IOException {
        _childProtocol.sendData(src, dst, dataPacket);
    }

    @Override
    public boolean receive(Callback callback) throws IOException {
        return _childProtocol.receive(new Callback() {
            @Override
            protected void onReceivePosition(String src, double latitude, double longitude, double altitude, float bearing, String comment) {
                throw new UnsupportedOperationException();
            }

            @Override
            protected void onReceivePcmAudio(String src, String dst, int codec, short[] pcmFrame) {
                throw new UnsupportedOperationException();
            }

            @Override
            protected void onReceiveCompressedAudio(String src, String dst, int codec2Mode, byte[] audioFrame) {
                Codec2.decode(_codec2Con, _playbackAudioBuffer, audioFrame);
                callback.onReceivePcmAudio(src, dst, codec2Mode, _playbackAudioBuffer);
            }

            @Override
            protected void onReceiveData(String src, String dst, byte[] data) {
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
        throw new UnsupportedOperationException();
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
        _codec2Mode = codecMode;
        _codec2Con = Codec2.create(codecMode);

        _audioBufferSize = Codec2.getSamplesPerFrame(_codec2Con);
        int codec2FrameSize = Codec2.getBitsSize(_codec2Con); // returns number of bytes

        _recordAudioEncodedBuffer = new char[codec2FrameSize];
        _playbackAudioBuffer = new short[_audioBufferSize];
    }
}
