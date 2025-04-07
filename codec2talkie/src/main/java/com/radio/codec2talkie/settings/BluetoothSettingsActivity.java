package com.radio.codec2talkie.settings;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.connect.BleGattWrapper;

import java.util.Arrays;

public class BluetoothSettingsActivity extends AppCompatActivity {

    private final static int SCAN_PERIOD = 5000;

    private BluetoothAdapter _btAdapter;
    private ArrayAdapter<String> _btArrayAdapter;
    private BluetoothLeScanner _btBleScanner;

    private SharedPreferences _sharedPreferences;
    private boolean _isBleEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connect);

        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        _isBleEnabled = SettingsWrapper.isBleTransport(_sharedPreferences);

        _btAdapter = BluetoothAdapter.getDefaultAdapter();
        _btArrayAdapter = new ArrayAdapter<>(this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item);
        _btBleScanner = _btAdapter.getBluetoothLeScanner();

        ListView _btDevicesList = findViewById(R.id.btDevicesList);
        _btDevicesList.setAdapter(_btArrayAdapter);
        _btDevicesList.setOnItemClickListener(onBtDeviceClickListener);

        findViewById(R.id.progressBarBt).setVisibility(View.INVISIBLE);
        findViewById(R.id.textViewConnectingBt).setVisibility(View.INVISIBLE);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        populateBondedDevices();
    }

    private final ScanCallback leScanCallback =
        new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                String deviceName = result.getDevice().getName() + " | " + result.getDevice().getAddress();
                if (_btArrayAdapter.getPosition(deviceName) == -1) {
                    _btArrayAdapter.add(deviceName);
                }
            }
        };

    private void populateBondedDevices() {
        _btArrayAdapter.clear();

        if (_isBleEnabled) {
            ScanFilter.Builder scanFilterBuilder = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(BleGattWrapper.BT_KISS_SERVICE_UUID));
            ScanFilter[] scanFilters = { scanFilterBuilder.build() };

            ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
            ScanSettings scanSettings = scanSettingsBuilder.build();

            _btBleScanner.startScan(Arrays.asList(scanFilters), scanSettings, leScanCallback);

            new Handler(Looper.getMainLooper()).postDelayed(() -> _btBleScanner.stopScan(leScanCallback), SCAN_PERIOD);

        } else {
            for (BluetoothDevice device : _btAdapter.getBondedDevices()) {
                _btArrayAdapter.add(device.getName() + " | " + device.getAddress());
            }
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
