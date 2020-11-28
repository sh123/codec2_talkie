package com.radio.codec2talkie;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class BluetoothConnectActivity extends AppCompatActivity {

    private final static int REQUEST_ENABLE_BT = 1;

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

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
            Toast.makeText(getApplicationContext(), "Bluetooth adapter is not found", Toast.LENGTH_SHORT).show();
        } else if (_bluetoothAdapter.isEnabled()) {
            populateBondedDevices();
        }
        else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            Toast.makeText(getApplicationContext(),"Bluetooth turned on", Toast.LENGTH_SHORT).show();
        }
    }

    private void populateBondedDevices() {
        _btArrayAdapter.clear();
        for (BluetoothDevice device : _bluetoothAdapter.getBondedDevices()) {
            _btArrayAdapter.add(device.getName() + " " + device.getAddress());
        }
    }

    private final AdapterView.OnItemClickListener onBtDeviceClickListener  = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String btName = (String)parent.getAdapter().getItem(position);
            String address = btName.substring(btName.length() - 17);

            Toast.makeText(getApplicationContext(),"Connecting to " + btName, Toast.LENGTH_SHORT).show();
            connectToBluetoothClient(address);
        }
    };

    private void connectToBluetoothClient(String address) {
        BluetoothDevice btDevice = _bluetoothAdapter.getRemoteDevice(address);
        try {
            _bluetoothSocket = btDevice.createRfcommSocketToServiceRecord(BTMODULEUUID);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Bluetooth socket creation failed", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            _bluetoothSocket.connect();
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Bluetooth socket connection failed", Toast.LENGTH_SHORT).show();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        super.onActivityResult(requestCode, resultCode, Data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                populateBondedDevices();
            } else {
            }
        }
    }
}