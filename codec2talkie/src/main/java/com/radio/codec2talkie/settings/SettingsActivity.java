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
            // audio
            PreferenceKeys.CODEC2_TX_FRAME_MAX_SIZE,
            PreferenceKeys.OPUS_BIT_RATE,
            // tnc
            PreferenceKeys.PORTS_TCP_IP_PORT,
            PreferenceKeys.PORTS_TCP_IP_RETRY_COUNT,
            PreferenceKeys.PORTS_TCP_IP_RETRY_DELAY,
            PreferenceKeys.PORTS_SOUND_MODEM_PREAMBLE,
            PreferenceKeys.PORTS_SOUND_MODEM_PTT_OFF_DELAY_MS,
            // kiss
            PreferenceKeys.KISS_EXTENSIONS_RADIO_FREQUENCY,
            PreferenceKeys.KISS_BASIC_P,
            PreferenceKeys.KISS_BASIC_SLOT_TIME,
            PreferenceKeys.KISS_BASIC_TX_DELAY,
            PreferenceKeys.KISS_BASIC_TX_TAIL,
            // aprs
            PreferenceKeys.APRS_LOCATION_SOURCE_GPS_UPDATE_TIME,
            PreferenceKeys.APRS_LOCATION_SOURCE_GPS_UPDATE_DISTANCE,
            PreferenceKeys.APRS_LOCATION_SOURCE_MANUAL_UPDATE_INTERVAL_MINUTES,
            PreferenceKeys.APRS_LOCATION_SOURCE_SMART_FAST_RATE,
            PreferenceKeys.APRS_LOCATION_SOURCE_SMART_FAST_SPEED,
            PreferenceKeys.APRS_LOCATION_SOURCE_SMART_SLOW_RATE,
            PreferenceKeys.APRS_LOCATION_SOURCE_SMART_SLOW_SPEED,
            PreferenceKeys.APRS_LOCATION_SOURCE_SMART_MIN_TURN_ANGLE,
            PreferenceKeys.APRS_LOCATION_SOURCE_SMART_MIN_TURN_TIME,
            PreferenceKeys.APRS_LOCATION_SOURCE_SMART_TURN_SLOPE,
            PreferenceKeys.APRS_IS_TCPIP_SERVER_PORT
    };

    private static final String[] _signedDecimalSettings = {
            // aprs
            PreferenceKeys.APRS_LOCATION_SOURCE_MANUAL_LAT,
            PreferenceKeys.APRS_LOCATION_SOURCE_MANUAL_LON
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

    public static class SettingsTncExtendedFragment extends PreferenceFragmentCompat
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_tnc_extended, null);
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

    public static class SettingsCodecFragment extends PreferenceFragmentCompat
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_codec, null);
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
