package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.protocol.ax25.AX25Packet;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.tools.DebugTools;
import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;

public class Ax25 implements Protocol {

    private static final String TAG = Ax25.class.getSimpleName();

    final Protocol _childProtocol;
    private String _digipath;
    private boolean _isVoax25Enabled;

    private ProtocolCallback _parentProtocolCallback;

    public Ax25(Protocol childProtocol) {
        _childProtocol = childProtocol;
    }

    @Override
    public void initialize(Transport transport, Context context, ProtocolCallback protocolCallback) throws IOException {
        _parentProtocolCallback = protocolCallback;
        _childProtocol.initialize(transport, context, _protocolCallback);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        // NOTE, may need to pass through sendData/sendAudio
        _digipath = sharedPreferences.getString(PreferenceKeys.APRS_DIGIPATH, "");
        _isVoax25Enabled = sharedPreferences.getBoolean(PreferenceKeys.APRS_VOAX25_ENABLE, false);
    }

    @Override
    public int getPcmAudioBufferSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendCompressedAudio(String src, String dst, int codec2Mode, byte[] frame) throws IOException {
        if (_isVoax25Enabled) {
            AX25Packet ax25Packet = new AX25Packet();
            ax25Packet.src = src;
            ax25Packet.dst = dst;
            ax25Packet.digipath = _digipath;
            ax25Packet.codec2Mode = codec2Mode;
            ax25Packet.isAudio = true;
            ax25Packet.rawData = frame;
            byte[] ax25Frame = ax25Packet.toBinary();
            if (ax25Frame == null) {
                Log.e(TAG, "Invalid source data for AX.25");
                _parentProtocolCallback.onProtocolTxError();
            } else {
                _childProtocol.sendCompressedAudio(src, dst, codec2Mode, ax25Frame);
            }
        } else {
            _childProtocol.sendCompressedAudio(src, dst, codec2Mode, frame);
        }
    }

    @Override
    public void sendPcmAudio(String src, String dst, int codec, short[] pcmFrame) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendData(String src, String dst, byte[] dataPacket) throws IOException {
        AX25Packet ax25Packet = new AX25Packet();
        ax25Packet.src = src;
        ax25Packet.dst = dst;
        ax25Packet.digipath = _digipath;
        ax25Packet.isAudio = false;
        ax25Packet.rawData = dataPacket;
        byte[] ax25Frame = ax25Packet.toBinary();
        if (ax25Frame == null) {
            Log.e(TAG, "Invalid source data for AX.25");
            _parentProtocolCallback.onProtocolTxError();
        } else {
            _childProtocol.sendData(src, dst, ax25Frame);
        }
    }

    @Override
    public boolean receive() throws IOException {
        return _childProtocol.receive();
    }

    ProtocolCallback _protocolCallback = new ProtocolCallback() {
        @Override
        protected void onReceivePosition(Position position) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void onReceivePcmAudio(String src, String dst, int codec, short[] pcmFrame) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void onReceiveCompressedAudio(String src, String dst, int codec2Mode, byte[] audioFrames) {
            AX25Packet ax25Data = new AX25Packet();
            ax25Data.fromBinary(audioFrames);
            if (ax25Data.isValid) {
                if (ax25Data.isAudio) {
                    _parentProtocolCallback.onReceiveCompressedAudio(ax25Data.src, ax25Data.dst, ax25Data.codec2Mode, ax25Data.rawData);
                } else {
                    _parentProtocolCallback.onReceiveData(ax25Data.src, ax25Data.dst, ax25Data.rawData);
                }
            } else {
                // fallback to raw audio if ax25 frame is invalid
                _parentProtocolCallback.onReceiveCompressedAudio(src, dst, codec2Mode, audioFrames);
            }
        }

        @Override
        protected void onReceiveData(String src, String dst, byte[] data) {
            _parentProtocolCallback.onReceiveData(src, dst, data);
        }

        @Override
        protected void onReceiveSignalLevel(short rssi, short snr) {
            _parentProtocolCallback.onReceiveSignalLevel(rssi, snr);
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
        protected void onTransmitData(String src, String dst, byte[] data) {
            _parentProtocolCallback.onTransmitData(src, dst, data);
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
        _childProtocol.flush();
    }

    @Override
    public void close() {
        _childProtocol.close();
    }
}
