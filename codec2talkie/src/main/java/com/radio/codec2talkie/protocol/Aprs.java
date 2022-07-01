package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.protocol.aprs.AprsTools;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;

public class Aprs implements Protocol {

    private final Protocol _childProtocol;

    private String _srcCallsign;
    private String _dstCallsign;

    private Callback _parentCallback;

    boolean _isVoax25Enabled;

    public Aprs(Protocol childProtocol) {
        _childProtocol = childProtocol;
    }

    @Override
    public void initialize(Transport transport, Context context, Callback callback) throws IOException {
        _parentCallback = callback;
        _childProtocol.initialize(transport, context, _protocolCallback);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        _isVoax25Enabled = sharedPreferences.getBoolean(PreferenceKeys.APRS_VOAX25_ENABLE, false);
        _srcCallsign = sharedPreferences.getString(PreferenceKeys.APRS_CALLSIGN, "NOCALL") + "-" +
                sharedPreferences.getString(PreferenceKeys.APRS_SSID, "0");
        _dstCallsign = "APZMDM";
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
        protected void onReceivePosition(String src, double latitude, double longitude, double altitude, float bearing, String comment) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void onReceivePcmAudio(String src, String dst, int codec, short[] pcmFrame) {
            String dstCallsign = AprsTools.isAprsSoftwareCallsign(dst) ? "ALL" : dst;
            _parentCallback.onReceivePcmAudio(src, dstCallsign, codec, pcmFrame);
        }

        @Override
        protected void onReceiveCompressedAudio(String src, String dst, int codec2Mode, byte[] audioFrame) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void onReceiveData(String src, String dst, byte[] data) {
            // process aprs data and call onReceivePosition if position packet is received
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
            String dstCallsign = AprsTools.isAprsSoftwareCallsign(dst) ? "ALL" : dst;
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
    public void sendPosition(double latitude, double longitude, double altitude, float bearing, String comment) {
        // TODO, implement
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
