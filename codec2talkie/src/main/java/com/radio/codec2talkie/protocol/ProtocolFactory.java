package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.settings.PreferenceKeys;

public class ProtocolFactory {

    public enum ProtocolType {
        RAW("RAW"),
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

    public static Protocol create(ProtocolType protocolType, int codec2ModeId, Context context) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

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
            case RAW:
            default:
                proto = new Raw();
                break;
        }

        if (scramblingEnabled) {
            proto = new ScramblerPipe(proto, scramblingKey);
        }
        if (aprsEnabled) {
            proto = new AX25(proto);
        }
        if (recordingEnabled) {
            proto = new RecorderPipe(proto, codec2ModeId);
        }

        return new AudioFrameAggregator(proto, codec2ModeId);
    }
}
