package com.radio.codec2talkie.bluetooth;

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
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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

    private BluetoothAdapter _btAdapter;
    private BluetoothSocket _btSocket;
    private ArrayAdapter<String> _btArrayAdapter;
    private String _btSelectedName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connect);

        _btAdapter = BluetoothAdapter.getDefaultAdapter();
        _btArrayAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item);
        ListView _btDevicesList = findViewById(R.id.btDevicesList);
        _btDevicesList.setAdapter(_btArrayAdapter);
        _btDevicesList.setOnItemClickListener(onBtDeviceClickListener);

        enableBluetooth();
    }

    private void enableBluetooth() {
        if (_btAdapter == null) {
            Message resultMsg = new Message();
            resultMsg.what = BT_ADAPTER_FAILURE;
            onBtStateChanged.sendMessage(resultMsg);
        } else if (_btAdapter.isEnabled()) {
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
        for (BluetoothDevice device : _btAdapter.getBondedDevices()) {
            _btArrayAdapter.add(device.getName() + " | " + device.getAddress());
        }
    }

    private void connectToBluetoothClient(String address) {

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
        }
    };

    private final AdapterView.OnItemClickListener onBtDeviceClickListener  = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            _btSelectedName = (String)parent.getAdapter().getItem(position);
            String address = _btSelectedName.substring(_btSelectedName.length() - 17);

            Toast.makeText(getApplicationContext(),"Connecting to " + _btSelectedName, Toast.LENGTH_LONG).show();
            connectToBluetoothClient(address);
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        super.onActivityResult(requestCode, resultCode, Data);
        if (requestCode == BT_ENABLE) {
            if (resultCode == RESULT_OK) {
                populateBondedDevices();
            } else if (resultCode == RESULT_CANCELED){
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }
}