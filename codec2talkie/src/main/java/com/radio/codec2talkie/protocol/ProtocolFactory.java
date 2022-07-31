package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.settings.PreferenceKeys;

public class ProtocolFactory {

    public enum ProtocolType {
        RAW("RAW"),
        HDLC("HDLC"),
        KISS("KISS"),
        KISS_BUFFERED("KISS BUF"),
        KISS_PARROT("KISS RPT");

        private final String _name;

        ProtocolType(String name) {
            _name = name;
        }

        @Override
        public String toString() {
            return _name;
        }
    }

    public static ProtocolType getBaseProtocolType(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        ProtocolFactory.ProtocolType protocolType;

        if (sharedPreferences.getString(PreferenceKeys.PORTS_TYPE, "loopback").equals("sound_modem")) {
            protocolType = ProtocolFactory.ProtocolType.HDLC;
        } else if (sharedPreferences.getBoolean(PreferenceKeys.KISS_ENABLED, true)) {
            if (sharedPreferences.getBoolean(PreferenceKeys.KISS_PARROT, false)) {
                protocolType = ProtocolFactory.ProtocolType.KISS_PARROT;
            }
            else if (sharedPreferences.getBoolean(PreferenceKeys.KISS_BUFFERED_ENABLED, false)) {
                protocolType = ProtocolFactory.ProtocolType.KISS_BUFFERED;
            }
            else {
                protocolType = ProtocolFactory.ProtocolType.KISS;
            }
        } else {
            protocolType = ProtocolFactory.ProtocolType.RAW;
        }
        return protocolType;
    }

    public static Protocol create(int codec2ModeId, Context context) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        ProtocolType protocolType = getBaseProtocolType(context);

        boolean recordingEnabled = sharedPreferences.getBoolean(PreferenceKeys.CODEC2_RECORDING_ENABLED, false);
        boolean scramblingEnabled = sharedPreferences.getBoolean(PreferenceKeys.KISS_SCRAMBLING_ENABLED, false);
        String scramblingKey = sharedPreferences.getString(PreferenceKeys.KISS_SCRAMBLER_KEY, "");
        boolean aprsEnabled = sharedPreferences.getBoolean(PreferenceKeys.APRS_ENABLED, false);

        Protocol proto;
        switch (protocolType) {
            case KISS:
                proto = new Kiss();
                break;
            case KISS_BUFFERED:
                proto = new KissBuffered();
                break;
            case KISS_PARROT:
                proto = new KissParrot();
                break;
            case HDLC:
                proto = new Hdlc(sharedPreferences);
                break;
            case RAW:
            default:
                proto = new Raw();
                break;
        }

        if (scramblingEnabled) {
            proto = new Scrambler(proto, scramblingKey);
        }
        if (aprsEnabled) {
            proto = new Ax25(proto);
        }
        if (recordingEnabled) {
            proto = new Recorder(proto, codec2ModeId);
        }

        proto = new AudioFrameAggregator(proto, codec2ModeId);
        proto = new AudioCodec2(proto, codec2ModeId);

        if (aprsEnabled) { // && protocolType != ProtocolType.RAW) {
            proto = new Aprs(proto);
        }
        return proto;
    }
}
