package com.radio.codec2talkie.rigctl;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.settings.PreferenceKeys;

public class RigCtlFactory {
    private static final String TAG = RigCtlFactory.class.getSimpleName();

    public static RigCtl create(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String rigName = sharedPreferences.getString(PreferenceKeys.PORTS_SOUND_MODEM_RIG, "Disabled");
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
