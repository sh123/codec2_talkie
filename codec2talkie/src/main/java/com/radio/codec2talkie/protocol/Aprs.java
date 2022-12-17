package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.protocol.aprs.AprsCallsign;
import com.radio.codec2talkie.protocol.aprs.AprsData;
import com.radio.codec2talkie.protocol.aprs.AprsDataFactory;
import com.radio.codec2talkie.protocol.aprs.AprsDataType;
import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.protocol.ax25.AX25Callsign;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.settings.SettingsWrapper;
import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;

public class Aprs implements Protocol {
    private static final String TAG = Aprs.class.getSimpleName();

    private final Protocol _childProtocol;

    private ProtocolCallback _parentProtocolCallback;

    private String _srcCallsign;
    private String _dstCallsign;
    private String _symbolCode;
    private String _status;
    private String _comment;
    private int _miceDigipath;
    private boolean _isVoax25Enabled;
    private boolean _isCompressed;
    private boolean _isBearingCourseEnabled;
    private boolean _isAltitudeEnabled;
    private int _privacyLevel;

    private AprsDataType _positionDataType;

    public Aprs(Protocol childProtocol) {
        _childProtocol = childProtocol;
    }

    @Override
    public void initialize(Transport transport, Context context, ProtocolCallback protocolCallback) throws IOException {
        _parentProtocolCallback = protocolCallback;
        _childProtocol.initialize(transport, context, _protocolCallback);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        _isVoax25Enabled = SettingsWrapper.isVoax25Enabled(sharedPreferences);

        _srcCallsign = AX25Callsign.formatCallsign(
                sharedPreferences.getString(PreferenceKeys.AX25_CALLSIGN, "NOCALL").toUpperCase(),
                sharedPreferences.getString(PreferenceKeys.AX25_SSID, "0"));
        _dstCallsign = "APZMDM";

        _symbolCode = sharedPreferences.getString(PreferenceKeys.APRS_SYMBOL, "/[");
        String packetFormat = sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_PACKET_FORMAT, "uncompressed");
        _status = sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_MIC_E_MESSAGE_TYPE, "off_duty");
        _comment = sharedPreferences.getString(PreferenceKeys.APRS_COMMENT, "off_duty");
        _privacyLevel = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.APRS_PRIVACY_POSITION_AMBIGUITY, "0"));
        _isAltitudeEnabled = sharedPreferences.getBoolean(PreferenceKeys.APRS_PRIVACY_ALTITUDE_ENABLED, false);
        _isBearingCourseEnabled = sharedPreferences.getBoolean(PreferenceKeys.APRS_PRIVACY_SPEED_ENABLED, false);
        _miceDigipath = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_MIC_E_DIGIPATH, "0"));
        _isCompressed = packetFormat.equals("compressed");

        AprsDataType.DataType dataType = packetFormat.equals("mic_e") ?
                AprsDataType.DataType.MIC_E : AprsDataType.DataType.POSITION_WITHOUT_TIMESTAMP_MSG;
        _positionDataType = new AprsDataType(dataType);
    }

    @Override
    public int getPcmAudioRecordBufferSize() {
        return _childProtocol.getPcmAudioRecordBufferSize();
    }

    @Override
    public void sendCompressedAudio(String src, String dst, int codec2Mode, byte[] frame) throws IOException {
        _childProtocol.sendCompressedAudio(src, dst, codec2Mode, frame);
    }

    @Override
    public void sendTextMessage(TextMessage textMessage) throws IOException {
        AprsDataType aprsDataType = new AprsDataType(AprsDataType.DataType.MESSAGE);
        AprsData aprsData = AprsDataFactory.create(aprsDataType);
        assert aprsData != null;
        aprsData.fromTextMessage(textMessage);
        if (aprsData.isValid()) {
            textMessage.src = _srcCallsign;
            sendData(_srcCallsign, _dstCallsign, null, aprsData.toBinary());
            _parentProtocolCallback.onTransmitTextMessage(textMessage);
        } else {
            Log.e(TAG, "Invalid APRS message");
            _parentProtocolCallback.onProtocolTxError();
        }
    }

    @Override
    public void sendPcmAudio(String src, String dst, int codec2Mode, short[] pcmFrame) throws IOException {
        if (_isVoax25Enabled) {
            _childProtocol.sendPcmAudio(src == null ? _srcCallsign : src, dst == null ? _dstCallsign : dst, codec2Mode, pcmFrame);
        } else {
            _childProtocol.sendPcmAudio(src, dst, codec2Mode, pcmFrame);
        }
    }

    @Override
    public void sendData(String src, String dst, String path, byte[] dataPacket) throws IOException {
        _childProtocol.sendData(src == null ? _srcCallsign : src, dst == null ? _dstCallsign : dst, path, dataPacket);
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
            String dstCallsign = new AprsCallsign(dst).isSoftware() ? "*" : dst;
            _parentProtocolCallback.onReceivePcmAudio(src, dstCallsign, codec, pcmFrame);
        }

        @Override
        protected void onReceiveCompressedAudio(String src, String dst, int codec2Mode, byte[] audioFrame) {
            _parentProtocolCallback.onReceiveCompressedAudio(src, dst, codec2Mode, audioFrame);
        }

        @Override
        protected void onReceiveTextMessage(TextMessage textMessage) {
            _parentProtocolCallback.onReceiveTextMessage(textMessage);
        }

        @Override
        protected void onReceiveData(String src, String dst, String path, byte[] data) {
            if (data.length == 0) return;
            AprsDataType dataType = new AprsDataType((char)data[0]);
            AprsData aprsData = AprsDataFactory.fromBinary(src, dst, path, data);
            if (aprsData != null && aprsData.isValid()) {
                if (dataType.isTextMessage()) {
                    TextMessage textMessage = aprsData.toTextMessage();
                    _parentProtocolCallback.onReceiveTextMessage(textMessage);
                    return;
                } else if (dataType.isPositionReport()) {
                    Position position = aprsData.toPosition();
                    if (position != null) {
                        _parentProtocolCallback.onReceivePosition(position);
                        return;
                    }
                }
            }
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
            String dstCallsign = new AprsCallsign(dst).isSoftware() ? "*" : dst;
            _parentProtocolCallback.onTransmitPcmAudio(src, dstCallsign, codec, frame);
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
        position.dstCallsign = _dstCallsign;
        position.srcCallsign = _srcCallsign;
        position.symbolCode = _symbolCode;
        position.comment = _comment;
        position.status = _status;
        position.isCompressed = _isCompressed;
        position.privacyLevel = _privacyLevel;
        position.isSpeedBearingEnabled = _isBearingCourseEnabled;
        position.isAltitudeEnabled = _isAltitudeEnabled;
        position.extDigipathSsid = _miceDigipath;
        AprsData aprsData = AprsDataFactory.create(_positionDataType);
        if (aprsData != null) {
            try {
                aprsData.fromPosition(position);
            } catch (Exception e) {
                e.printStackTrace();
                _parentProtocolCallback.onProtocolTxError();
                return;
            }
            if (aprsData.isValid()) {
                sendData(position.srcCallsign, position.dstCallsign, null, aprsData.toBinary());
                _parentProtocolCallback.onTransmitPosition(position);
            } else {
                Log.e(TAG, "Invalid APRS data");
                _parentProtocolCallback.onProtocolTxError();
            }
        }
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
