package com.radio.codec2talkie.bluetooth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.radio.codec2talkie.R;

import java.io.IOException;
import java.util.UUID;

public class BluetoothConnectActivity extends AppCompatActivity {

    private final static int BT_ENABLE = 1;
    private final static int BT_CONNECT_SUCCESS = 2;
    private final static int BT_CONNECT_FAILURE = 3;
    private final static int BT_SOCKET_FAILURE = 4;
    private final static int BT_ADAPTER_FAILURE = 5;

    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final int BtAddressLength = 17;

    private ProgressBar _progressBarBt;
    private ListView _btDevicesList;

    private BluetoothAdapter _btAdapter;
    private BluetoothSocket _btSocket;
    private ArrayAdapter<String> _btArrayAdapter;
    private String _btSelectedName;
    private String _btDefaultName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connect);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        _btDefaultName = sharedPreferences.getString("ports_bt_client_name", null);

        _btAdapter = BluetoothAdapter.getDefaultAdapter();
        _btArrayAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item);

        _btDevicesList = findViewById(R.id.btDevicesList);
        _btDevicesList.setAdapter(_btArrayAdapter);
        _btDevicesList.setOnItemClickListener(onBtDeviceClickListener);
        _btDevicesList.setVisibility(View.INVISIBLE);

        _progressBarBt = findViewById(R.id.progressBarBt);
        _progressBarBt.setVisibility(View.VISIBLE);
        ObjectAnimator.ofInt(_progressBarBt, "progress", 10)
                .setDuration(300)
                .start();

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
            startActivityForResult(enableBtIntent, BT_ENABLE);
            Toast.makeText(getApplicationContext(),"Bluetooth turned on", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeviceList() {
        if (_btDevicesList.getVisibility() == View.INVISIBLE) {
            _progressBarBt.setVisibility(View.INVISIBLE);
            _btDevicesList.setVisibility(View.VISIBLE);
        }
    }

    private void showProgressBar() {
        if (_progressBarBt.getVisibility() == View.INVISIBLE) {
            _progressBarBt.setVisibility(View.VISIBLE);
            _btDevicesList.setVisibility(View.INVISIBLE);
        }
    }

    private void connectOrPopulateDevices() {
        if (_btDefaultName != null && !_btDefaultName.trim().isEmpty()) {
            _btSelectedName = _btDefaultName;
            connectToBluetoothClient(addressFromDisplayName(_btDefaultName));
        } else {
            populateBondedDevices();
        }
    }

    private void populateBondedDevices() {
        _btArrayAdapter.clear();
        for (BluetoothDevice device : _btAdapter.getBondedDevices()) {
            _btArrayAdapter.add(device.getName() + " | " + device.getAddress());
        }
        showDeviceList();
    }

    private void connectToBluetoothClient(String address) {
        showProgressBar();

        new Thread() {
            @Override
            public void run() {
                BluetoothDevice btDevice = _btAdapter.getRemoteDevice(address);
                Message resultMsg = Message.obtain();
                resultMsg.what = BT_CONNECT_SUCCESS;
                try {
                    _btSocket = btDevice.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
                } catch (IOException e) {
                    resultMsg.what = BT_SOCKET_FAILURE;
                    onBtStateChanged.sendMessage(resultMsg);
                    return;
                }
                try {
                    _btSocket.connect();
                } catch (IOException e) {
                    resultMsg.what = BT_CONNECT_FAILURE;
                }
                onBtStateChanged.sendMessage(resultMsg);
            }
        }.start();
    }

    private final Handler onBtStateChanged = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            String toastMsg;
            if (msg.what == BT_CONNECT_FAILURE) {
                toastMsg = "Bluetooth connect failed";
                // connection to default device failed, fall back
                if (_btDefaultName != null && !_btDefaultName.trim().isEmpty()) {
                    _btDefaultName = null;
                    populateBondedDevices();
                    return;
                }
            } else if (msg.what == BT_SOCKET_FAILURE) {
                toastMsg = "Bluetooth socket failed";
            } else if (msg.what == BT_ADAPTER_FAILURE) {
                toastMsg = "Bluetooth adapter is not found";
            } else {
                toastMsg = "Connected";
                SocketHandler.setSocket(_btSocket);
                Intent resultIntent = new Intent();
                resultIntent.putExtra("name", _btSelectedName);
                setResult(Activity.RESULT_OK, resultIntent);
            }
            Toast.makeText(getBaseContext(), toastMsg, Toast.LENGTH_SHORT).show();
            if (msg.what == BT_CONNECT_SUCCESS) {
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
            Toast.makeText(getApplicationContext(),"Connecting to " + _btSelectedName, Toast.LENGTH_LONG).show();
            connectToBluetoothClient(addressFromDisplayName(_btSelectedName));
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        super.onActivityResult(requestCode, resultCode, Data);
        if (requestCode == BT_ENABLE) {
            if (resultCode == RESULT_OK) {
                connectOrPopulateDevices();
            } else if (resultCode == RESULT_CANCELED){
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }
}