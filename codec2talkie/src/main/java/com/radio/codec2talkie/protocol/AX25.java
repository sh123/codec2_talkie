package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;

public class AX25 implements Protocol {

    public static class AX25Data {
        public String src;
        public String dst;
        public String digipath;
        public boolean isAudio;
        public byte[] rawData;
    }

    public static class AX25Callsign {
        public String callsign;
        public String ssid;
    }

    final Protocol _childProtocol;
    private String _digipath;

    public AX25(Protocol childProtocol) {
        _childProtocol = childProtocol;
    }

    @Override
    public void initialize(Transport transport, Context context) throws IOException {
        _childProtocol.initialize(transport, context);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        _digipath = sharedPreferences.getString(PreferenceKeys.APRS_DIGIPATH, "");
    }

    @Override
    public void sendAudio(String src, String dst, byte[] frame) throws IOException {
        AX25Data data = new AX25Data();
        data.src = src;
        data.dst = dst;
        data.digipath = _digipath;
        data.isAudio = true;
        data.rawData = frame;
        byte[] ax25Frame = buildPacket(data);
        if (ax25Frame != null) {
            _childProtocol.sendAudio(src, dst, frame);
        }
    }

    @Override
    public void sendData(String src, String dst, byte[] dataPacket) throws IOException {
        AX25Data data = new AX25Data();
        data.src = src;
        data.dst = dst;
        data.digipath = _digipath;
        data.isAudio = false;
        data.rawData = dataPacket;
        byte[] ax25Frame = buildPacket(data);
        if (ax25Frame != null) {
            _childProtocol.sendData(src, dst, dataPacket);
        }
    }

    @Override
    public boolean receive(Callback callback) throws IOException {
        return _childProtocol.receive(new Callback() {
            @Override
            protected void onReceiveAudioFrames(String src, String dst, byte[] audioFrames) {
                AX25Data ax25Data = parsePacket(audioFrames);
                if (ax25Data != null) {
                    if (ax25Data.isAudio) {
                        callback.onReceiveAudioFrames(ax25Data.src, ax25Data.dst, audioFrames);
                    } else {
                        callback.onReceiveData(ax25Data.src, ax25Data.dst, audioFrames);
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
    public void flush() throws IOException {
        _childProtocol.flush();
    }

    @Override
    public void close() {
        _childProtocol.close();
    }

    private byte[] buildPacket(AX25Data data) {
        return data.rawData;
    }

    private AX25Data parsePacket(byte[] data) {
        AX25Data ax25Data = new AX25Data();
        ax25Data.src = null;
        ax25Data.dst = null;
        ax25Data.digipath = null;
        ax25Data.isAudio = true;
        ax25Data.rawData = data;
        return ax25Data;
    }
}
