package com.radio.codec2talkie.rigctl;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.connect.UsbPortHandler;
import com.radio.codec2talkie.settings.PreferenceKeys;

public class RigCtlFactory {
    private static final String TAG = RigCtlFactory.class.getSimpleName();

    public static final String RIG_DISABLED = "Disabled";
    public static final String RIG_PHONE_TORCH = "PhoneTorch";

    public static RigCtl create(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String rigName = sharedPreferences.getString(PreferenceKeys.PORTS_SOUND_MODEM_RIG, RIG_DISABLED);
        if (UsbPortHandler.getPort() == null && !rigName.equals(RIG_PHONE_TORCH))
            return new Disabled();
        try {
            Class<?> loadClass = Class.forName(String.format("com.radio.codec2talkie.rigctl.%s", rigName));
            Log.i(TAG, "Using rig " + rigName);
            return (RigCtl)loadClass.newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
            return new Disabled();
        }
    }
}
