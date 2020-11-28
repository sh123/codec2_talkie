package com.radio.codec2talkie;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private final static int REQUEST_CONNECT_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startBluetoothConnectActivity();
    }

    protected void startBluetoothConnectActivity() {
        Intent bluetoothConnectIntent = new Intent(this, BluetoothConnectActivity.class);
        startActivityForResult(bluetoothConnectIntent, REQUEST_CONNECT_BT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        super.onActivityResult(requestCode, resultCode, Data);
        if (requestCode == REQUEST_CONNECT_BT) {
            if (resultCode == RESULT_OK) {
            } else {
            }
        }
    }
}