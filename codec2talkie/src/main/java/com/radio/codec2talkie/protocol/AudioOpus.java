package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.transport.Transport;
import com.radio.opus.Opus;

import java.io.IOException;

public class AudioOpus implements Protocol {

    private static final String TAG = AudioOpus.class.getSimpleName();

    private final Protocol _childProtocol;

    private static final int SAMPLE_RATE = 8000;

    private int _pcmFrameSize;

    private char[] _recordAudioEncodedBuffer;
    private short[] _playbackAudioBuffer;

    private ProtocolCallback _parentProtocolCallback;

    private long _opusCon;
    private int _audioBufferSize;

    public AudioOpus(Protocol childProtocol) {
        _childProtocol = childProtocol;
    }

    @Override
    public void initialize(Transport transport, Context context, ProtocolCallback protocolCallback) throws IOException {
        _parentProtocolCallback = protocolCallback;
        _childProtocol.initialize(transport, context, _protocolCallback);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int bitRate = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.OPUS_BIT_RATE, "3200"));
        int complexity = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.OPUS_COMPLEXITY, "5"));
        float pcmFrameDuration = Float.parseFloat(sharedPreferences.getString(PreferenceKeys.OPUS_FRAME_SIZE, "40"));

        int superFrameSize = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.CODEC2_TX_FRAME_MAX_SIZE, "48"));
        _pcmFrameSize = (int)(SAMPLE_RATE / 1000 * pcmFrameDuration);
        _audioBufferSize = 10*_pcmFrameSize;

        _playbackAudioBuffer = new short[_audioBufferSize];
        _recordAudioEncodedBuffer = new char[superFrameSize];

        _opusCon = Opus.create(SAMPLE_RATE, 1, Opus.OPUS_APPLICATION_VOIP, bitRate, complexity);
        if (_opusCon == 0) {
            Log.e(TAG, "Failed to create opus");
        }
        Log.i(TAG, "Opus is initialized, pcm frame size: " + _pcmFrameSize + ", super frame size: " + superFrameSize);
    }

    @Override
    public int getPcmAudioRecordBufferSize() {
        return _pcmFrameSize;
    }

    @Override
    public void sendCompressedAudio(String src, String dst, byte[] frame) throws IOException {
        _childProtocol.sendCompressedAudio(src, dst, frame);
    }

    @Override
    public void sendTextMessage(TextMessage textMessage) throws IOException {
        _childProtocol.sendTextMessage(textMessage);
    }

    @Override
    public void sendPcmAudio(String src, String dst, short[] pcmFrame) throws IOException {
        _parentProtocolCallback.onTransmitPcmAudio(src, dst, pcmFrame);
        int encodedBytesCnt = Opus.encode(_opusCon, pcmFrame, _pcmFrameSize, _recordAudioEncodedBuffer);
        if (encodedBytesCnt == 0) {
            Log.w(TAG, "Nothing was encoded");
            return;
        }
        if (encodedBytesCnt > 0) {
            byte[] frame = new byte[encodedBytesCnt];
            for (int i = 0; i < encodedBytesCnt; i++) {
                frame[i] = (byte) _recordAudioEncodedBuffer[i];
            }
            Log.v(TAG, "pcm count: " + pcmFrame.length + ", encoded bytes count: " + encodedBytesCnt);
            _childProtocol.sendCompressedAudio(src, dst, frame);
        } else {
            Log.e(TAG, "Encode error: " + encodedBytesCnt);
            _parentProtocolCallback.onProtocolTxError();
        }
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
        protected void onReceivePcmAudio(String src, String dst, short[] pcmFrame) {
            _parentProtocolCallback.onReceivePcmAudio(src, dst, pcmFrame);
        }

        @Override
        protected void onReceiveCompressedAudio(String src, String dst, byte[] audioEncodedFrame) {
            int decodedSamplesCnt = Opus.decode(_opusCon, audioEncodedFrame, _playbackAudioBuffer, _audioBufferSize);
            Log.v(TAG, "encoded frame size: " + audioEncodedFrame.length + ", decoded samples count:" + decodedSamplesCnt);
            if (decodedSamplesCnt == 0) {
                Log.w(TAG, "Nothing was decoded");
                return;
            }
            short [] decodedSamples = new short[decodedSamplesCnt];
            if (decodedSamplesCnt > 0) {
                System.arraycopy(_playbackAudioBuffer, 0, decodedSamples, 0, decodedSamplesCnt);
            } else {
                Log.e(TAG, "Decode error: " + decodedSamplesCnt);
                _parentProtocolCallback.onProtocolRxError();
            }
            _parentProtocolCallback.onReceivePcmAudio(src, dst, decodedSamples);
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
        Opus.destroy(_opusCon);
        _childProtocol.close();
    }
}
