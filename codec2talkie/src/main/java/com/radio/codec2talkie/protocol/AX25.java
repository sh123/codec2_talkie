package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.protocol.ax25.AX25Packet;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;

public class AX25 implements Protocol {

    final Protocol _childProtocol;
    private String _digipath;

    public AX25(Protocol childProtocol) {
        _childProtocol = childProtocol;
    }

    @Override
    public void initialize(Transport transport, Context context) throws IOException {
        _childProtocol.initialize(transport, context);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        // NOTE, may need to pass through sendData/sendAudio
        _digipath = sharedPreferences.getString(PreferenceKeys.APRS_DIGIPATH, "");
    }

    @Override
    public void sendAudio(String src, String dst, int codec2Mode, byte[] frame) throws IOException {
        AX25Packet ax25Packet = new AX25Packet();
        ax25Packet.src = src;
        ax25Packet.dst = dst;
        ax25Packet.digipath = _digipath;
        ax25Packet.codec2Mode = codec2Mode;
        ax25Packet.isAudio = true;
        ax25Packet.rawData = frame;
        byte[] ax25Frame = ax25Packet.toBinary();
        if (ax25Frame != null) {
            _childProtocol.sendAudio(src, dst, codec2Mode, frame);
        }
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
        if (ax25Frame != null) {
            _childProtocol.sendData(src, dst, dataPacket);
        }
    }

    @Override
    public boolean receive(Callback callback) throws IOException {
        return _childProtocol.receive(new Callback() {
            @Override
            protected void onReceiveAudioFrames(String src, String dst, int codec2Mode, byte[] audioFrames) {
                AX25Packet ax25Data = new AX25Packet();
                ax25Data.fromBinary(audioFrames);
                if (ax25Data.isValid) {
                    if (ax25Data.isAudio) {
                        callback.onReceiveAudioFrames(ax25Data.src, ax25Data.dst, ax25Data.codec2Mode, audioFrames);
                    } else {
                        callback.onReceiveData(ax25Data.src, ax25Data.dst, audioFrames);
                    }
                } else {
                    callback.onProtocolRxError();
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
}
