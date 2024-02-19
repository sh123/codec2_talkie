package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.settings.SettingsWrapper;

public class ProtocolFactory {

    public enum ProtocolType {
        RAW("RAW"),
        HDLC("HDLC"),
        KISS("KISS"),
        KISS_BUFFERED("KISS BUF"),
        KISS_PARROT("KISS RPT"),
        FREEDV("FREEDV");

        private final String _name;

        ProtocolType(String name) {
            _name = name;
        }

        @NonNull
        @Override
        public String toString() {
            return _name;
        }
    }

    public static ProtocolType getBaseProtocolType(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        ProtocolFactory.ProtocolType protocolType;

        // Use HDLC instead of KISS for the sound modem as we are the modem
        if (SettingsWrapper.isSoundModemEnabled(sharedPreferences)) {
            if (SettingsWrapper.isFreeDvSoundModemModulation(sharedPreferences)) {
                protocolType = ProtocolType.FREEDV;
            } else {
                protocolType = ProtocolFactory.ProtocolType.HDLC;
            }
        } else if (SettingsWrapper.isKissProtocolEnabled(sharedPreferences)) {
            if (SettingsWrapper.isKissParrotEnabled(sharedPreferences)) {
                protocolType = ProtocolFactory.ProtocolType.KISS_PARROT;
            }
            else if (SettingsWrapper.isKissBufferedModeEnabled(sharedPreferences)) {
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

    public static Protocol create(Context context) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        ProtocolType protocolType = getBaseProtocolType(context);

        boolean recordingEnabled = SettingsWrapper.isCodec2RecorderEnabled(sharedPreferences);
        boolean scramblingEnabled = SettingsWrapper.isKissScramblerEnabled(sharedPreferences);
        String scramblingKey = SettingsWrapper.getKissScramblerKey(sharedPreferences);
        boolean aprsEnabled = SettingsWrapper.isAprsEnabled(sharedPreferences);
        boolean aprsIsEnabled = SettingsWrapper.isAprsIsEnabled(sharedPreferences);
        boolean freedvEnabled = SettingsWrapper.isFreeDvSoundModemModulation(sharedPreferences);
        boolean codec2Enabled = SettingsWrapper.isCodec2Enabled(sharedPreferences);
        boolean isCustomPrefixEnabled = SettingsWrapper.isCustomPrefixEnabled(sharedPreferences);

        // "root" protocol
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
            case FREEDV:
                proto = new Freedv();
                break;
            case RAW:
            default:
                proto = new Raw();
                break;
        }

        if (isCustomPrefixEnabled) {
            proto = new CustomDataPrefix(proto, sharedPreferences);
        }
        if (scramblingEnabled) {
            proto = new Scrambler(proto, scramblingKey);
        }
        if (aprsEnabled) {
            proto = new Ax25(proto);
        }
        if (!freedvEnabled) {
            if (codec2Enabled) {
                if (recordingEnabled) {
                    proto = new Recorder(proto, sharedPreferences);
                }
                proto = new AudioCodec2FrameAggregator(proto, sharedPreferences);
                proto = new AudioCodec2(proto, sharedPreferences);
            } else {
                proto = new AudioOpus(proto);
            }
        }

        if (aprsEnabled) {
            if (aprsIsEnabled) {
                proto = new AprsIs(proto);
            }
            proto = new Aprs(proto);
        }
        return proto;
    }
}
