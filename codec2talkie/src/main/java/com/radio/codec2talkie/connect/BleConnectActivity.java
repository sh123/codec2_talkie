package com.radio.codec2talkie.connect;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.settings.PreferenceKeys;

import java.util.Arrays;

public class BleConnectActivity extends AppCompatActivity {

    private final static int SCAN_PERIOD = 5000;

    public final static int BT_GATT_CONNECT_SUCCESS = 1;
    public final static int BT_GATT_CONNECT_FAILURE = 2;
    public final static int BT_ADAPTER_FAILURE = 3;
    public final static int BT_SCAN_COMPLETED = 4;
    public final static int BT_SERVICES_DISCOVERED = 5;
    public final static int BT_SERVICES_DISCOVER_FAILURE = 6;
    public final static int BT_UNSUPPORTED_CHARACTERISTICS = 7;

    private static final int BtAddressLength = 17;

    private ProgressBar _progressBarBt;
    private ListView _btDevicesList;
    private TextView _textViewConnectingBt;

    private BluetoothLeScanner _btBleScanner;
    private BluetoothAdapter _btAdapter;
    private BleGattWrapper _btGatt;
    private ArrayAdapter<String> _btArrayAdapter;
    private String _btSelectedName;
    private String _btDefaultName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connect);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        _btDefaultName = sharedPreferences.getString(PreferenceKeys.PORTS_BT_CLIENT_NAME, null);

        _btAdapter = BluetoothAdapter.getDefaultAdapter();
        _btArrayAdapter = new ArrayAdapter<>(this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item);

        _btDevicesList = findViewById(R.id.btDevicesList);
        _btDevicesList.setAdapter(_btArrayAdapter);
        _btDevicesList.setOnItemClickListener(onBtDeviceClickListener);
        _btDevicesList.setVisibility(View.INVISIBLE);

        _textViewConnectingBt = findViewById(R.id.textViewConnectingBt);
        _textViewConnectingBt.setVisibility(View.VISIBLE);

        _progressBarBt = findViewById(R.id.progressBarBt);
        _progressBarBt.setVisibility(View.VISIBLE);
        ObjectAnimator.ofInt(_progressBarBt, "progress", 10)
                .setDuration(300)
                .start();

        _btBleScanner = _btAdapter.getBluetoothLeScanner();
        enableBluetooth();
    }

    private void enableBluetooth() {
        if (_btAdapter == null) {
            Message resultMsg = new Message();
            resultMsg.what = BT_ADAPTER_FAILURE;
            onBtStateChanged.sendMessage(resultMsg);
        } else if (_btAdapter.isEnabled()) {
            connectOrPopulateDevices();
        }
        else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            _enableBtLauncher.launch(enableBtIntent);
            Toast.makeText(getApplicationContext(), getString(R.string.bt_turned_on), Toast.LENGTH_SHORT).show();
        }
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

    private final ActivityResultLauncher<Intent> _enableBtLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            int resultCode = result.getResultCode();
            if (resultCode == RESULT_OK) {
                connectOrPopulateDevices();
            } else if (resultCode == RESULT_CANCELED){
                setResult(RESULT_CANCELED);
                finish();
            }
        });

    private void showDeviceList() {
        if (_btDevicesList.getVisibility() == View.INVISIBLE) {
            _progressBarBt.setVisibility(View.INVISIBLE);
            _textViewConnectingBt.setVisibility(View.INVISIBLE);
            _btDevicesList.setVisibility(View.VISIBLE);
        }
    }

    private void showProgressBar() {
        if (_progressBarBt.getVisibility() == View.INVISIBLE) {
            _progressBarBt.setVisibility(View.VISIBLE);
            _textViewConnectingBt.setVisibility(View.VISIBLE);
            _btDevicesList.setVisibility(View.INVISIBLE);
        }
    }

    private void connectOrPopulateDevices() {
        if (_btDefaultName != null && !_btDefaultName.trim().isEmpty()) {
            _btSelectedName = _btDefaultName;
            gattConnectToBluetoothClient(addressFromDisplayName(_btDefaultName));
        } else {
            startDevicesScan();
        }
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

    private void startDevicesScan() {
        _btArrayAdapter.clear();

        ScanFilter.Builder scanFilterBuilder = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(BleGattWrapper.BT_KISS_SERVICE_UUID));
        ScanFilter[] scanFilters = { scanFilterBuilder.build() };

        ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
        ScanSettings scanSettings = scanSettingsBuilder.build();

        _btBleScanner = _btAdapter.getBluetoothLeScanner();
        _btBleScanner.startScan(Arrays.asList(scanFilters), scanSettings, leScanCallback);

        Message resultMsg = new Message();
        resultMsg.what = BT_SCAN_COMPLETED;
        onBtStateChanged.sendMessageDelayed(resultMsg, SCAN_PERIOD);
    }

    private void gattConnectToBluetoothClient(String address) {
        showProgressBar();
        BluetoothDevice device = _btAdapter.getRemoteDevice(address);
        _btGatt = new BleGattWrapper(getApplicationContext(), onBtStateChanged);
        _btGatt.connect(device);
    }

    private final Handler onBtStateChanged = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            String toastMsg;
            if (msg.what == BT_GATT_CONNECT_FAILURE) {
                toastMsg = getString(R.string.bt_connect_failed);
                // connection to default device failed, fall back
                if (_btDefaultName != null && !_btDefaultName.trim().isEmpty()) {
                    _btDefaultName = null;
                    startDevicesScan();
                    return;
                }
            } else if (msg.what == BT_SCAN_COMPLETED) {
                toastMsg = getString(R.string.bt_ble_scan_completed);
                _btBleScanner.stopScan(leScanCallback);
            } else if (msg.what == BT_GATT_CONNECT_SUCCESS) {
                toastMsg = getString(R.string.bt_le_gatt_connected);
            } else if (msg.what == BT_SERVICES_DISCOVER_FAILURE) {
                toastMsg = getString(R.string.bt_le_services_discover_failure);
            } else if (msg.what == BT_UNSUPPORTED_CHARACTERISTICS) {
                toastMsg = getString(R.string.bt_le_unsupported_characteristics);
            } else {
                toastMsg = getString(R.string.bt_le_services_discovered);
                BleHandler.setGatt(_btGatt);
                BleHandler.setName(_btSelectedName);
                Intent resultIntent = new Intent();
                resultIntent.putExtra("name", _btSelectedName);
                setResult(Activity.RESULT_OK, resultIntent);
            }
            Toast.makeText(getBaseContext(), toastMsg, Toast.LENGTH_SHORT).show();
            if (msg.what == BT_SERVICES_DISCOVERED) {
                finish();
            }
            showDeviceList();
        }
    };

    private String addressFromDisplayName(String displayName) {
        return displayName.substring(displayName.length() - BtAddressLength);
    }

    private final AdapterView.OnItemClickListener onBtDeviceClickListener  = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            _btSelectedName = (String)parent.getAdapter().getItem(position);
            Toast.makeText(getApplicationContext(), getString(R.string.bt_connecting_to, _btSelectedName), Toast.LENGTH_LONG).show();
            gattConnectToBluetoothClient(addressFromDisplayName(_btSelectedName));
        }
    };
}