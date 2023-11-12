package com.radio.codec2talkie.settings;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.R;

public class SettingsActivity extends AppCompatActivity
{
    private static final String[] _numberSettings = {
            "codec2_tx_frame_max_size",
            "kiss_extension_radio_frequency",
            "aprs_location_source_gps_update_time",
            "aprs_location_source_gps_update_distance",
            "aprs_location_source_manual_update_time",
            "aprs_location_source_smart_fast_speed",
            "aprs_location_source_smart_fast_rate",
            "aprs_location_source_smart_slow_speed",
            "aprs_location_source_smart_slow_rate",
            "aprs_location_source_smart_min_turn_time",
            "aprs_location_source_smart_min_turn_angle",
            "aprs_location_source_smart_turn_slope",
            "kiss_basic_persistence",
            "kiss_basic_slot_time",
            "kiss_basic_tx_delay",
            "kiss_basic_tx_tail",
            "kiss_scrambler_iterations",
            "ports_tcp_ip_port",
            "ports_tcp_ip_retry_count",
            "ports_tcp_ip_retry_delay",
            "ports_sound_modem_preamble",
            "ports_sound_modem_ptt_off_delay_ms",
            "aprs_is_tcpip_server_port"
    };

    private static final String[] _signedDecimalSettings = {
            "aprs_location_source_manual_lat",
            "aprs_location_source_manual_lon"
    };

    public static void setNumberInputType(PreferenceManager preferenceManager) {
        for (String key : _numberSettings) {
            EditTextPreference editTextPreference = preferenceManager.findPreference(key);
            if (editTextPreference != null)
                editTextPreference.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
        }
        for (String key : _signedDecimalSettings) {
            EditTextPreference editTextPreference = preferenceManager.findPreference(key);
            if (editTextPreference != null)
                editTextPreference.setOnBindEditTextListener(editText -> editText.setInputType(
                        InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
            setNumberInputType(getPreferenceManager());
        }
    }

    public static class SettingsRadioFragment extends PreferenceFragmentCompat
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_radio, null);
            setNumberInputType(getPreferenceManager());

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
            setNumberInputType(getPreferenceManager());
        }
    }

    public static class SettingsTcpIpFragment extends PreferenceFragmentCompat
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_tcpip, null);
            setNumberInputType(getPreferenceManager());
        }
    }

    public static class SettingsSoundModemFragment extends PreferenceFragmentCompat
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_sound_modem, null);
            setNumberInputType(getPreferenceManager());
        }
    }

    public static class SettingsUsbFragment extends PreferenceFragmentCompat
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_usb, null);
            setNumberInputType(getPreferenceManager());
        }
    }

    public static class SettingsAprsLocationFragment extends PreferenceFragmentCompat
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_aprs_location, null);
            setNumberInputType(getPreferenceManager());
        }
    }

    public static class SettingsPositionPrivacyFragment extends PreferenceFragmentCompat
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_aprs_privacy, null);
            setNumberInputType(getPreferenceManager());
        }
    }

    public static class SettingsAprsIsFragment extends PreferenceFragmentCompat
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_aprs_is, null);
            setNumberInputType(getPreferenceManager());
        }
    }
}
