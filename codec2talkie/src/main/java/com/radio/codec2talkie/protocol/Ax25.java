package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.protocol.ax25.AX25Packet;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;

public class Ax25 implements Protocol {

    private static final String TAG = Ax25.class.getSimpleName();

    final Protocol _childProtocol;
    private String _digipath;
    private boolean _isVoax25Enabled;

    public Ax25(Protocol childProtocol) {
        _childProtocol = childProtocol;
    }

    @Override
    public void initialize(Transport transport, Context context) throws IOException {
        _childProtocol.initialize(transport, context);
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
    public boolean sendCompressedAudio(String src, String dst, int codec2Mode, byte[] frame) throws IOException {
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
                return false;
            } else {
                return _childProtocol.sendCompressedAudio(src, dst, codec2Mode, ax25Frame);
            }
        } else {
            return _childProtocol.sendCompressedAudio(src, dst, codec2Mode, frame);
        }
    }

    @Override
    public boolean sendPcmAudio(String src, String dst, int codec, short[] pcmFrame) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean sendData(String src, String dst, byte[] dataPacket) throws IOException {
        AX25Packet ax25Packet = new AX25Packet();
        ax25Packet.src = src;
        ax25Packet.dst = dst;
        ax25Packet.digipath = _digipath;
        ax25Packet.isAudio = false;
        ax25Packet.rawData = dataPacket;
        byte[] ax25Frame = ax25Packet.toBinary();
        if (ax25Frame == null) {
            Log.e(TAG, "Invalid source data for AX.25");
            return false;
        } else {
            return _childProtocol.sendData(src, dst, dataPacket);
        }
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
                throw new UnsupportedOperationException();
            }

            @Override
            protected void onReceiveCompressedAudio(String src, String dst, int codec2Mode, byte[] audioFrames) {
                AX25Packet ax25Data = new AX25Packet();
                ax25Data.fromBinary(audioFrames);
                if (ax25Data.isValid) {
                    if (ax25Data.isAudio) {
                        parentCallback.onReceiveCompressedAudio(ax25Data.src, ax25Data.dst, ax25Data.codec2Mode, ax25Data.rawData);
                    } else {
                        parentCallback.onReceiveData(ax25Data.src, ax25Data.dst, audioFrames);
                    }
                } else {
                    // fallback to raw audio is ax25 frame is invalid
                    parentCallback.onReceiveCompressedAudio(src, dst, codec2Mode, audioFrames);
                }
            }

            @Override
            protected void onReceiveData(String src, String dst, byte[] data) {
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
