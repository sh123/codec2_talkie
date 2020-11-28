package com.radio.codec2talkie;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class BluetoothConnectActivity extends AppCompatActivity {

    private final static int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connect);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        enableBluetooth(bluetoothAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data){
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
            }
            else {
            }
        }
    }

    private void enableBluetooth(BluetoothAdapter bluetoothAdapter){
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth adapter is not found", Toast.LENGTH_SHORT).show();
        } else if (bluetoothAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(),"Bluetooth is already on", Toast.LENGTH_SHORT).show();
        }
        else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            Toast.makeText(getApplicationContext(),"Bluetooth turned on", Toast.LENGTH_SHORT).show();
        }
    }
}