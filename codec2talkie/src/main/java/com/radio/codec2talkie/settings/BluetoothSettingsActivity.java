package com.radio.codec2talkie.settings;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.R;

public class BluetoothSettingsActivity extends AppCompatActivity {

    private BluetoothAdapter _btAdapter;
    private ArrayAdapter<String> _btArrayAdapter;

    SharedPreferences _sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connect);

        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        _btAdapter = BluetoothAdapter.getDefaultAdapter();
        _btArrayAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item);

        ListView _btDevicesList = findViewById(R.id.btDevicesList);
        _btDevicesList.setAdapter(_btArrayAdapter);
        _btDevicesList.setOnItemClickListener(onBtDeviceClickListener);

        findViewById(R.id.progressBarBt).setVisibility(View.INVISIBLE);

        populateBondedDevices();
    }

    private void populateBondedDevices() {
        _btArrayAdapter.clear();
        for (BluetoothDevice device : _btAdapter.getBondedDevices()) {
            _btArrayAdapter.add(device.getName() + " | " + device.getAddress());
        }
    }

    private final AdapterView.OnItemClickListener onBtDeviceClickListener  = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String _btSelectedName = (String) parent.getAdapter().getItem(position);
            SharedPreferences.Editor editor = _sharedPreferences.edit();
            editor.putString(PreferenceKeys.PORTS_BT_CLIENT_NAME, _btSelectedName);
            editor.apply();
            finish();
        }
    };
}
