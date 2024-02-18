package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.tools.AudioTools;
import com.radio.codec2talkie.transport.Transport;
import com.ustadmobile.codec2.Codec2;

import java.io.IOException;
import java.util.Arrays;

public class AudioCodec2FrameAggregator implements Protocol {

    private static final String TAG = AudioCodec2FrameAggregator.class.getSimpleName();

    private final Protocol _childProtocol;

    private int _outputBufferSize;
    private int _outputBufferPos;
    private byte[] _outputBuffer;

    private int _codec2FrameSize;

    private String _lastSrc;
    private String _lastDst;

    private final SharedPreferences _sharedPreferences;
    private ProtocolCallback _parentProtocolCallback;

    public AudioCodec2FrameAggregator(Protocol childProtocol, SharedPreferences sharedPreferences) {
        _childProtocol = childProtocol;
        _sharedPreferences = sharedPreferences;
    }

    @Override
    public void initialize(Transport transport, Context context, ProtocolCallback protocolCallback) throws IOException {
        _parentProtocolCallback = protocolCallback;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        _outputBufferSize = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.CODEC2_TX_FRAME_MAX_SIZE, "48"));
        _outputBuffer = new byte[_outputBufferSize];
        _outputBufferPos = 0;
        _childProtocol.initialize(transport, context, _protocolCallback);

        String codec2ModeName = _sharedPreferences.getString(PreferenceKeys.CODEC2_MODE, context.getResources().getStringArray(R.array.codec2_modes)[0]);
        int codec2ModeId = AudioTools.extractCodec2ModeId(codec2ModeName);
        _codec2FrameSize = getPcmAudioBufferSize(codec2ModeId);
    }

    @Override
    public int getPcmAudioRecordBufferSize() {
        return _childProtocol.getPcmAudioRecordBufferSize();

    }
    public int getPcmAudioBufferSize(int codec2ModeId) {
        long codec2Con = Codec2.create(codec2ModeId);
        int codec2FrameSize = Codec2.getBitsSize(codec2Con); // returns number of bytes
        Codec2.destroy(codec2Con);
        return codec2FrameSize;
    }

    @Override
    public void sendCompressedAudio(String src, String dst, byte[] frame) throws IOException {
        if ( _outputBufferPos + frame.length > _outputBufferSize) {
            _childProtocol.sendCompressedAudio(src, dst, Arrays.copyOf(_outputBuffer, _outputBufferPos));
            _outputBufferPos = 0;
        }
        _lastSrc = src;
        _lastDst = dst;
        System.arraycopy(frame, 0, _outputBuffer, _outputBufferPos, frame.length);
        _outputBufferPos += frame.length;
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
        protected void onReceiveCompressedAudio(String src, String dst, byte[] audioFrames) {
            if (audioFrames.length % _codec2FrameSize != 0) {
                Log.e(TAG, "Ignoring audio frame of wrong size: " + audioFrames.length);
                _parentProtocolCallback.onProtocolRxError();
            } else {
                // split by audio frame
                byte[] audioFrame = new byte[_codec2FrameSize];
                for (int i = 0; i < audioFrames.length; i += _codec2FrameSize) {
                    for (int j = 0; j < _codec2FrameSize && (j + i) < audioFrames.length; j++) {
                        audioFrame[j] = audioFrames[i + j];
                    }
                    _parentProtocolCallback.onReceiveCompressedAudio(src, dst, audioFrame);
                }
            }
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
    public void sendPosition(Position position) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flush() throws IOException {
        if (_outputBufferPos > 0) {
            _childProtocol.sendCompressedAudio(_lastSrc, _lastDst, Arrays.copyOf(_outputBuffer, _outputBufferPos));
            _outputBufferPos = 0;
        }
        _childProtocol.flush();
    }

    @Override
    public void close() {
        _childProtocol.close();
    }
}
