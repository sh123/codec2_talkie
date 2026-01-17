package com.radio.codec2talkie.protocol.ciphers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.settings.PreferenceKeys;

public class ProtocolCipherFactory {
    private static final String TAG = ProtocolCipherFactory.class.getSimpleName();
    private static final String CIPHER_DISABLED = "disabled";

    public static boolean isEnabled(SharedPreferences sharedPreferences) {
        String cipherName = sharedPreferences.getString(PreferenceKeys.KISS_CIPHER_TYPE, CIPHER_DISABLED);
        return !cipherName.equals(CIPHER_DISABLED);
    }

    public static ProtocolCipher create(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String cipherName = sharedPreferences.getString(PreferenceKeys.KISS_CIPHER_TYPE, CIPHER_DISABLED);
        String cipherKey = sharedPreferences.getString(PreferenceKeys.KISS_CIPHER_KEY, "");
        if (cipherName.equals(CIPHER_DISABLED)) return null;
        try {
            Class<?> loadClass = Class.forName(String.format("com.radio.codec2talkie.protocol.ciphers.%s", cipherName));
            Log.i(TAG, "Using cipher " + cipherName);
            ProtocolCipher protocolCipher = (ProtocolCipher)loadClass.newInstance();
            protocolCipher.setKey(cipherKey);
            return protocolCipher;
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
            return null;
        }
    }
}
