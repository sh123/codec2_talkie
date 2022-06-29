package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.transport.Transport;
import com.ustadmobile.codec2.Codec2;

import java.io.IOException;
import java.util.Arrays;

public class AudioFrameAggregator implements Protocol {

    private static final String TAG = AudioFrameAggregator.class.getSimpleName();

    private final String DEFAULT_TX_FRAME_MAX_SIZE = "48";
    private final Protocol _childProtocol;

    private int _outputBufferSize;
    private int _outputBufferPos;
    private byte[] _outputBuffer;

    private int _codec2FrameSize;
    private int _codec2ModeId;

    private String _lastSrc;
    private String _lastDst;
    private int _lastCodec2Mode;

    public AudioFrameAggregator(Protocol childProtocol, int codec2ModeId) {
        _childProtocol = childProtocol;
        _codec2ModeId = codec2ModeId;
        _codec2FrameSize = getPcmAudioBufferSize(codec2ModeId);
    }

    @Override
    public void initialize(Transport transport, Context context) throws IOException {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        _outputBufferSize = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.CODEC2_TX_FRAME_MAX_SIZE, DEFAULT_TX_FRAME_MAX_SIZE));
        _outputBuffer = new byte[_outputBufferSize];
        _outputBufferPos = 0;
        _childProtocol.initialize(transport, context);
    }

    @Override
    public int getPcmAudioBufferSize(int codec2ModeId) {
        long codec2Con = Codec2.create(codec2ModeId);
        int codec2FrameSize = Codec2.getBitsSize(codec2Con); // returns number of bytes
        Codec2.destroy(codec2Con);
        return codec2FrameSize;
    }

    @Override
    public void sendCompressedAudio(String src, String dst, int codec2Mode, byte[] frame) throws IOException {
        if ( _outputBufferPos + frame.length >= _outputBufferSize) {
            _childProtocol.sendCompressedAudio(src, dst, codec2Mode, Arrays.copyOf(_outputBuffer, _outputBufferPos));
            _lastSrc = src;
            _lastDst = dst;
            _lastCodec2Mode = codec2Mode;
            _outputBufferPos = 0;
        }
        System.arraycopy(frame, 0, _outputBuffer, _outputBufferPos, frame.length);
        _outputBufferPos += frame.length;
    }

    @Override
    public void sendPcmAudio(String src, String dst, int codec, short[] pcmFrame) {
        throw new UnsupportedOperationException();
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
            protected void onReceiveCompressedAudio(String src, String dst, int codec2Mode, byte[] audioFrames) {
                if (audioFrames.length % _codec2FrameSize != 0) {
                    Log.e(TAG, "Ignoring audio frame of wrong size: " + audioFrames.length);
                    callback.onProtocolRxError();
                } else {
                    // split by audio frame
                    byte[] audioFrame = new byte[_codec2FrameSize];
                    for (int i = 0; i < audioFrames.length; i += _codec2FrameSize) {
                        for (int j = 0; j < _codec2FrameSize && (j + i) < audioFrames.length; j++) {
                            audioFrame[j] = audioFrames[i + j];
                        }
                        callback.onReceiveCompressedAudio(src, dst, codec2Mode, audioFrame);
                    }
                }
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
        if (_outputBufferPos > 0) {
            _childProtocol.sendCompressedAudio(_lastSrc, _lastDst, _lastCodec2Mode, Arrays.copyOf(_outputBuffer, _outputBufferPos));
            _outputBufferPos = 0;
        }
        _childProtocol.flush();
    }

    @Override
    public void close() {
        _childProtocol.close();
    }
}
