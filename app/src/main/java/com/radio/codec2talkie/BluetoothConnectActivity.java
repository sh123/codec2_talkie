package com.radio.codec2talkie;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class BluetoothConnectActivity extends AppCompatActivity {

    private final static int BT_ENABLE = 1;
    private final static int BT_CONNECT_SUCCESS = 2;
    private final static int BT_CONNECT_FAILURE = 3;
    private final static int BT_SOCKET_FAILURE = 4;
    private final static int BT_ADAPTER_FAILURE = 5;

    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter _bluetoothAdapter;
    private BluetoothSocket _bluetoothSocket;
    private ArrayAdapter<String> _btArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connect);

        _bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        _btArrayAdapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item);
        ListView _btDevicesList = (ListView) findViewById(R.id.btDevicesList);
        _btDevicesList.setAdapter(_btArrayAdapter);
        _btDevicesList.setOnItemClickListener(onBtDeviceClickListener);

        enableBluetooth();
    }

    private void enableBluetooth() {
        if (_bluetoothAdapter == null) {
            Message resultMsg = new Message();
            resultMsg.what = BT_ADAPTER_FAILURE;
            _onBtStateChanged.sendMessage(resultMsg);
        } else if (_bluetoothAdapter.isEnabled()) {
            populateBondedDevices();
        }
        else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, BT_ENABLE);
            Toast.makeText(getApplicationContext(),"Bluetooth turned on", Toast.LENGTH_SHORT).show();
        }
    }

    private void populateBondedDevices() {
        _btArrayAdapter.clear();
        for (BluetoothDevice device : _bluetoothAdapter.getBondedDevices()) {
            _btArrayAdapter.add(device.getName() + " " + device.getAddress());
        }
    }

    private void connectToBluetoothClient(String address) {

        new Thread() {
            @Override
            public void run() {
                BluetoothDevice btDevice = _bluetoothAdapter.getRemoteDevice(address);
                Message resultMsg = Message.obtain();
                resultMsg.what = BT_CONNECT_SUCCESS;
                try {
                    _bluetoothSocket = btDevice.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
                } catch (IOException e) {
                    resultMsg.what = BT_SOCKET_FAILURE;
                    _onBtStateChanged.sendMessage(resultMsg);
                    return;
                }
                try {
                    _bluetoothSocket.connect();
                } catch (IOException e) {
                    resultMsg.what = BT_CONNECT_FAILURE;
                }
                _onBtStateChanged.sendMessage(resultMsg);
            }
        }.start();
    }

    private final Handler _onBtStateChanged = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            String toastMsg;
            if (msg.what == BT_CONNECT_FAILURE) {
                toastMsg = "Bluetooth connect failed";
            } else if (msg.what == BT_SOCKET_FAILURE) {
                toastMsg = "Bluetooth socket failed";
            } else if (msg.what == BT_ADAPTER_FAILURE) {
                toastMsg = "Bluetooth adapter is not found";
            } else {
                toastMsg = "Connected";
                setResult(Activity.RESULT_OK);
            }
            Toast.makeText(getBaseContext(), toastMsg, Toast.LENGTH_SHORT).show();
            if (msg.what == BT_CONNECT_SUCCESS) {
                finish();
            }
        }
    };

    private final AdapterView.OnItemClickListener onBtDeviceClickListener  = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String btName = (String)parent.getAdapter().getItem(position);
            String address = btName.substring(btName.length() - 17);

            Toast.makeText(getApplicationContext(),"Connecting to " + btName, Toast.LENGTH_LONG).show();
            connectToBluetoothClient(address);
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        super.onActivityResult(requestCode, resultCode, Data);
        if (requestCode == BT_ENABLE) {
            if (resultCode == RESULT_OK) {
                populateBondedDevices();
            } else {
            }
        }
    }
}