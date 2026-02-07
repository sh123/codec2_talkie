package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.protocol.ax25.AX25Callsign;
import com.radio.codec2talkie.protocol.ax25.AX25Packet;
import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.settings.SettingsWrapper;
import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;

public class Ax25 implements Protocol {

    private static final String TAG = Ax25.class.getSimpleName();

    final Protocol _childProtocol;

    private String _myCallsign;
    private String _digipath;
    private boolean _isVoax25Enabled;
    private boolean _isDigiRepeaterEnabled;
    private boolean _useTextPackets;

    private ProtocolCallback _parentProtocolCallback;

    public Ax25(Protocol childProtocol) {
        _childProtocol = childProtocol;
    }

    @Override
    public void initialize(Transport transport, Context context, ProtocolCallback protocolCallback) throws IOException {
        _parentProtocolCallback = protocolCallback;
        _childProtocol.initialize(transport, context, _protocolCallback);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        _myCallsign = SettingsWrapper.getMyCallsignWithSsid(sharedPreferences);
        // NOTE, may need to pass through sendData/sendAudio
        _digipath = sharedPreferences.getString(PreferenceKeys.AX25_DIGIPATH, "").toUpperCase();
        _isVoax25Enabled = SettingsWrapper.isVoax25Enabled(sharedPreferences);
        _useTextPackets = SettingsWrapper.isTextPacketsEnabled(sharedPreferences);
        _isDigiRepeaterEnabled = sharedPreferences.getBoolean(PreferenceKeys.AX25_DIGIREPEATER_ENABLED, false);
    }

    @Override
    public int getPcmAudioRecordBufferSize() {
        return _childProtocol.getPcmAudioRecordBufferSize();
    }

    @Override
    public void sendCompressedAudio(String src, String dst, byte[] frame) throws IOException {
        if (_isVoax25Enabled) {
            AX25Packet ax25Packet = new AX25Packet();
            ax25Packet.src = src;
            ax25Packet.dst = dst;
            ax25Packet.digipath = _digipath;
            ax25Packet.isAudio = true;
            ax25Packet.rawData = frame;
            byte[] ax25Frame = ax25Packet.toBinary();
            if (ax25Frame == null) {
                Log.e(TAG, "Cannot convert AX.25 voice packet to binary");
                _parentProtocolCallback.onProtocolTxError();
            } else {
                _childProtocol.sendCompressedAudio(src, dst, ax25Frame);
            }
        } else {
            _childProtocol.sendCompressedAudio(src, dst, frame);
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
        _parentProtocolCallback.onTransmitData(src, dst, _digipath, dataPacket);
        AX25Packet ax25Packet = new AX25Packet();
        ax25Packet.src = src;
        ax25Packet.dst = dst;
        ax25Packet.digipath = path == null ? _digipath : path;
        ax25Packet.isAudio = false;
        ax25Packet.rawData = dataPacket;
        byte[] ax25Frame = _useTextPackets ? ax25Packet.toTextBinary() : ax25Packet.toBinary();
        if (ax25Frame == null) {
            Log.e(TAG, "Cannot convert AX.25 data packet to binary");
            _parentProtocolCallback.onProtocolTxError();
        } else {
            _childProtocol.sendData(src, dst, ax25Packet.digipath, ax25Frame);
            _parentProtocolCallback.onTransmitLog(ax25Packet.toString());
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
        protected void onReceiveCompressedAudio(String src, String dst, byte[] audioFrames)  {
            AX25Packet ax25Data = new AX25Packet();
            ax25Data.fromBinary(audioFrames);
            if (ax25Data.isValid) {
                if (ax25Data.isAudio) {
                    _parentProtocolCallback.onReceiveCompressedAudio(ax25Data.src, ax25Data.dst, ax25Data.rawData);
                } else {
                    _parentProtocolCallback.onReceiveLog(ax25Data.toString());
                    _parentProtocolCallback.onReceiveData(ax25Data.src, ax25Data.dst, ax25Data.digipath, ax25Data.rawData);
                    if (_isDigiRepeaterEnabled) digiRepeat(ax25Data);
                }
            } else {
                // fallback to raw audio if ax25 frame is invalid
                _parentProtocolCallback.onReceiveCompressedAudio(src, dst, audioFrames);
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

    private void digiRepeat(AX25Packet ax25Packet) {
        if (ax25Packet.src.equals(_myCallsign)) return;
        if (!ax25Packet.digiRepeat()) return;
        byte[] ax25Frame = ax25Packet.toBinary();
        if (ax25Frame == null) {
            Log.e(TAG, "Cannot convert AX.25 digi repeated packet to binary");
            _parentProtocolCallback.onProtocolTxError();
        } else {
            try {
                _childProtocol.sendData(ax25Packet.src, ax25Packet.dst, ax25Packet.digipath, ax25Frame);
            } catch (IOException e) {
                Log.e(TAG, "Cannot send AX.25 digi repeated packet");
                e.printStackTrace();
                _parentProtocolCallback.onProtocolTxError();
                return;
            }
            _parentProtocolCallback.onTransmitLog(ax25Packet.toString());
        }
    }
}
