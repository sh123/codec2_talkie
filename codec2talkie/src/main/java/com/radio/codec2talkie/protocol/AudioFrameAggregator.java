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

    private final int _codec2FrameSize;

    public AudioFrameAggregator(Protocol childProtocol, int codec2ModeId) {
        _childProtocol = childProtocol;

        long codec2Con = Codec2.create(codec2ModeId);
        _codec2FrameSize = Codec2.getBitsSize(codec2Con); // returns number of bytes
        Codec2.destroy(codec2Con);
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
    public void send(byte[] frame) throws IOException {
        if ( _outputBufferPos + frame.length >= _outputBufferSize) {
            _childProtocol.send(Arrays.copyOf(_outputBuffer, _outputBufferPos));
            _outputBufferPos = 0;
        }
        System.arraycopy(frame, 0, _outputBuffer, _outputBufferPos, frame.length);
        _outputBufferPos += frame.length;
    }

    @Override
    public boolean receive(Callback callback) throws IOException {
        return _childProtocol.receive(new Callback() {
            @Override
            protected void onReceiveAudioFrames(byte[] audioFrames) {
                if (audioFrames.length % _codec2FrameSize != 0) {
                    Log.e(TAG, "Ignoring audio frame of wrong size: " + audioFrames.length);
                    callback.onProtocolError();
                } else {
                    // split by audio frame
                    byte[] audioFrame = new byte[_codec2FrameSize];
                    for (int i = 0; i < audioFrames.length; i += _codec2FrameSize) {
                        for (int j = 0; j < _codec2FrameSize && (j + i) < audioFrames.length; j++) {
                            audioFrame[j] = audioFrames[i + j];
                        }
                        callback.onReceiveAudioFrames(audioFrame);
                    }
                }
            }

            @Override
            protected void onReceiveSignalLevel(byte[] rawData) {
                callback.onReceiveSignalLevel(rawData);
            }

            @Override
            protected void onProtocolError() {
                callback.onProtocolError();
            }
        });
    }

    @Override
    public void flush() throws IOException {
        _childProtocol.send(Arrays.copyOf(_outputBuffer, _outputBufferPos));
        _outputBufferPos = 0;
        _childProtocol.flush();
    }
}
