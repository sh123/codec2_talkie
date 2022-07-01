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

    public Aprs(Protocol childProtocol) {
        _childProtocol = childProtocol;
    }

    @Override
    public void initialize(Transport transport, Context context) throws IOException {
        _childProtocol.initialize(transport, context);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        _srcCallsign = sharedPreferences.getString(PreferenceKeys.APRS_CALLSIGN, "NOCALL") + "-" +
                sharedPreferences.getString(PreferenceKeys.APRS_SSID, "0");
        _dstCallsign = "APZMDM";
    }

    @Override
    public int getPcmAudioBufferSize() {
        return _childProtocol.getPcmAudioBufferSize();
    }

    @Override
    public boolean sendCompressedAudio(String src, String dst, int codec2Mode, byte[] frame) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean sendPcmAudio(String src, String dst, int codec2Mode, short[] pcmFrame) throws IOException {
        return _childProtocol.sendPcmAudio(src == null ? _srcCallsign : src, dst == null ? _dstCallsign : dst, codec2Mode, pcmFrame);
    }

    @Override
    public boolean sendData(String src, String dst, byte[] dataPacket) throws IOException {
        return _childProtocol.sendData(src == null ? _srcCallsign : src, dst == null ? _dstCallsign : dst, dataPacket);
    }

    @Override
    public boolean receive(Callback parentCallback) throws IOException {
        return _childProtocol.receive(new Callback() {
            @Override
            protected void onReceivePosition(String src, double latitude, double longitude, double altitude, float bearing, String comment) {
                throw new UnsupportedOperationException();
            }

            @Override
            protected void onReceivePcmAudio(String src, String dst, int codec, short[] pcmFrame) {
                String dstCallsign = AprsTools.isAprsSoftwareCallsign(dst) ? "ALL" : dst;
                parentCallback.onReceivePcmAudio(src, dstCallsign, codec, pcmFrame);
            }

            @Override
            protected void onReceiveCompressedAudio(String src, String dst, int codec2Mode, byte[] audioFrame) {
                throw new UnsupportedOperationException();
            }

            @Override
            protected void onReceiveData(String src, String dst, byte[] data) {
                // process aprs data and call onReceivePosition if position packet is received
                parentCallback.onReceiveData(src, dst, data);
            }

            @Override
            protected void onReceiveSignalLevel(short rssi, short snr) {
                parentCallback.onReceiveSignalLevel(rssi, snr);
            }

            @Override
            protected void onProtocolRxError() {
                parentCallback.onProtocolRxError();
            }
        });
    }

    @Override
    public boolean sendPosition(double latitude, double longitude, double altitude, float bearing, String comment) {
        // TODO, implement
        return true;
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
