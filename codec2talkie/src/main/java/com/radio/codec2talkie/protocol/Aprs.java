package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.audio.AudioProcessor;
import com.radio.codec2talkie.protocol.aprs.AprsCallsign;
import com.radio.codec2talkie.protocol.aprs.AprsData;
import com.radio.codec2talkie.protocol.aprs.AprsDataFactory;
import com.radio.codec2talkie.protocol.aprs.AprsDataType;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.protocol.ax25.AX25Callsign;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;

public class Aprs implements Protocol {
    private static final String TAG = Aprs.class.getSimpleName();

    private final Protocol _childProtocol;

    private Callback _parentCallback;

    private String _srcCallsign;
    private String _dstCallsign;
    private String _symbolCode;
    private String _status;
    private String _comment;
    private boolean _isVoax25Enabled;
    private boolean _isCompressed;

    private AprsDataType _positionDataType;

    public Aprs(Protocol childProtocol) {
        _childProtocol = childProtocol;
    }

    @Override
    public void initialize(Transport transport, Context context, Callback callback) throws IOException {
        _parentCallback = callback;
        _childProtocol.initialize(transport, context, _protocolCallback);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        _isVoax25Enabled = sharedPreferences.getBoolean(PreferenceKeys.APRS_VOAX25_ENABLE, false);

        _srcCallsign = AX25Callsign.formatCallsign(
                sharedPreferences.getString(PreferenceKeys.APRS_CALLSIGN, "NOCALL"),
                sharedPreferences.getString(PreferenceKeys.APRS_SSID, "0"));
        _dstCallsign = "APZMDM";

        _symbolCode = sharedPreferences.getString(PreferenceKeys.APRS_SYMBOL, "/[");
        String packetFormat = sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_PACKET_FORMAT, "uncompressed");
        _status = sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_MIC_E_MESSAGE_TYPE, "off_duty");
        _comment = sharedPreferences.getString(PreferenceKeys.APRS_COMMENT, "off_duty");
        _isCompressed = packetFormat.equals("compressed");

        AprsDataType.DataType dataType = packetFormat.equals("mic_e") ?
                AprsDataType.DataType.MIC_E : AprsDataType.DataType.POSITION_WITHOUT_TIMESTAMP_MSG;
        _positionDataType = new AprsDataType(dataType);
    }

    @Override
    public int getPcmAudioBufferSize() {
        return _childProtocol.getPcmAudioBufferSize();
    }

    @Override
    public void sendCompressedAudio(String src, String dst, int codec2Mode, byte[] frame) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendPcmAudio(String src, String dst, int codec2Mode, short[] pcmFrame) throws IOException {
        if (_isVoax25Enabled) {
            // just add callsigns and AX.25 will handle the rest
            _childProtocol.sendPcmAudio(src == null ? _srcCallsign : src, dst == null ? _dstCallsign : dst, codec2Mode, pcmFrame);
        } else {
            _childProtocol.sendPcmAudio(src, dst, codec2Mode, pcmFrame);
        }
    }

    @Override
    public void sendData(String src, String dst, byte[] dataPacket) throws IOException {
        _childProtocol.sendData(src == null ? _srcCallsign : src, dst == null ? _dstCallsign : dst, dataPacket);
    }

    @Override
    public boolean receive() throws IOException {
        return _childProtocol.receive();
    }

    Callback _protocolCallback = new Callback() {
        @Override
        protected void onReceivePosition(Position position) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void onReceivePcmAudio(String src, String dst, int codec, short[] pcmFrame) {
            String dstCallsign = new AprsCallsign(dst).isSoftware() ? "ALL" : dst;
            _parentCallback.onReceivePcmAudio(src, dstCallsign, codec, pcmFrame);
        }

        @Override
        protected void onReceiveCompressedAudio(String src, String dst, int codec2Mode, byte[] audioFrame) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void onReceiveData(String src, String dst, byte[] data) {
            AprsData aprsData = AprsDataFactory.fromBinary(data);
            if (aprsData != null && aprsData.isValid()) {
                Position position = aprsData.toPosition();
                if (position != null) {
                    _parentCallback.onReceivePosition(position);
                    return;
                }
            }
            _parentCallback.onReceiveData(src, dst, data);
        }

        @Override
        protected void onReceiveSignalLevel(short rssi, short snr) {
            _parentCallback.onReceiveSignalLevel(rssi, snr);
        }

        @Override
        protected void onReceiveLog(String logData) {
            _parentCallback.onReceiveLog(logData);
        }

        @Override
        protected void onTransmitPcmAudio(String src, String dst, int codec, short[] frame) {
            String dstCallsign = new AprsCallsign(dst).isSoftware() ? "ALL" : dst;
            _parentCallback.onTransmitPcmAudio(src, dstCallsign, codec, frame);
        }

        @Override
        protected void onTransmitCompressedAudio(String src, String dst, int codec, byte[] frame) {
            _parentCallback.onTransmitCompressedAudio(src, dst, codec, frame);
        }

        @Override
        protected void onTransmitData(String src, String dst, byte[] data) {
            _parentCallback.onTransmitData(src, dst, data);
        }

        @Override
        protected void onTransmitLog(String logData) {
            _parentCallback.onTransmitLog(logData);
        }

        @Override
        protected void onProtocolRxError() {
            _parentCallback.onProtocolRxError();
        }

        @Override
        protected void onProtocolTxError() {
            _parentCallback.onProtocolTxError();
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
        AprsData aprsData = AprsDataFactory.create(_positionDataType);
        if (aprsData != null) {
            aprsData.fromPosition(position);
            if (aprsData.isValid()) {
                sendData(position.srcCallsign, position.dstCallsign, aprsData.toBinary());
            } else {
                Log.e(TAG, "Send position protocol error");
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
