package com.radio.codec2talkie.settings;

import android.content.SharedPreferences;

import com.radio.codec2talkie.rigctl.RigCtlFactory;
import com.radio.codec2talkie.transport.TransportFactory;

public class SettingsWrapper {

    public static boolean isSoundModemRigDisabled(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString(PreferenceKeys.PORTS_SOUND_MODEM_RIG,
                RigCtlFactory.RIG_DISABLED).equals(RigCtlFactory.RIG_DISABLED);
    }

    public static boolean isSoundModemEnabled(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString(PreferenceKeys.PORTS_TYPE,
                TransportFactory.TransportType.LOOPBACK.toString()).toUpperCase().equals(
                        TransportFactory.TransportType.SOUND_MODEM.toString());
    }

    public static boolean isLoopbackTransport(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString(PreferenceKeys.PORTS_TYPE,
                TransportFactory.TransportType.LOOPBACK.toString()).toUpperCase().equals(
                TransportFactory.TransportType.LOOPBACK.toString());
    }

    public static boolean isBleTransport(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString(PreferenceKeys.PORTS_TYPE,
                TransportFactory.TransportType.LOOPBACK.toString()).toUpperCase().equals(
                TransportFactory.TransportType.BLE.toString());
    }

    public static TransportFactory.TransportType getCurrentTransportType(SharedPreferences sharedPreferences) {
        return TransportFactory.TransportType.valueOf(sharedPreferences.getString(PreferenceKeys.PORTS_TYPE,
                TransportFactory.TransportType.LOOPBACK.toString()).toUpperCase());
    }

    public static boolean isFreeDvSoundModemModulation(SharedPreferences sharedPreferences) {
        return isSoundModemEnabled(sharedPreferences) &&
                sharedPreferences.getString(PreferenceKeys.PORTS_SOUND_MODEM_TYPE, "1200").startsWith("F");
    }

    public static int getFreeDvSoundModemModulation(SharedPreferences sharedPreferences) {
        String modemType = sharedPreferences.getString(PreferenceKeys.PORTS_SOUND_MODEM_TYPE, "1200");
        if (modemType.startsWith("F")) {
            return Integer.parseInt(modemType.substring(1));
        }
        return -1;
    }

    public static int getFskSpeed(SharedPreferences sharedPreferences) {
        String modemType = sharedPreferences.getString(PreferenceKeys.PORTS_SOUND_MODEM_TYPE, "1200");
        if (!modemType.startsWith("F")) {
            return Integer.parseInt(modemType);
        }
        return -1;
    }

    public static boolean isKissProtocolEnabled(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean(PreferenceKeys.KISS_ENABLED, true);
    }

    public static boolean isKissParrotEnabled(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean(PreferenceKeys.KISS_PARROT, false);
    }

    public static boolean isKissBufferedModeEnabled(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean(PreferenceKeys.KISS_BUFFERED_ENABLED, false);
    }

    public static boolean isCodec2RecorderEnabled(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean(PreferenceKeys.CODEC2_RECORDING_ENABLED, false);
    }

    public static boolean isKissScramblerEnabled(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean(PreferenceKeys.KISS_SCRAMBLING_ENABLED, false);
    }

    public static boolean isKissExtensionEnabled(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean(PreferenceKeys.KISS_EXTENSIONS_ENABLED, false);
    }

    public static String getKissScramblerKey(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString(PreferenceKeys.KISS_SCRAMBLER_KEY, "");
    }

    public static boolean isAprsEnabled(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean(PreferenceKeys.APRS_ENABLED, false);
    }

    public static boolean isVoax25Enabled(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean(PreferenceKeys.AX25_VOAX25_ENABLE, false) &&
                !isFreeDvSoundModemModulation(sharedPreferences);   // no voax25 in freedv
    }
}
