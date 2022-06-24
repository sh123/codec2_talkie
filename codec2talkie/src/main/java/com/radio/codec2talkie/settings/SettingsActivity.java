package com.radio.codec2talkie.settings;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.radio.codec2talkie.MainActivity;
import com.radio.codec2talkie.R;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
        }
    }

    public static class SettingsRadioFragment extends PreferenceFragmentCompat
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_radio, null);

            Preference rebootPreference = findPreference("kiss_extension_reboot");
            assert rebootPreference != null;
            rebootPreference.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(PreferenceKeys.KISS_EXTENSIONS_ACTION_REBOOT_REQUESTED);
                requireContext().sendBroadcast(intent);
                return false;
            });
        }
    }

    public static class SettingsKissBasicFragment extends PreferenceFragmentCompat
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_kiss, null);
        }
    }

    public static class SettingsTcpIpFragment extends PreferenceFragmentCompat
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_tcpip, null);
        }
    }

    public static class SettingsUsbFragment extends PreferenceFragmentCompat
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_usb, null);
        }
    }

    public static class SettingsAprsLocationFragment extends PreferenceFragmentCompat
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_aprs_location, null);
        }
    }

    public static class SettingsPositionPrivacyFragment extends PreferenceFragmentCompat
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_aprs_privacy, null);
        }
    }
}
