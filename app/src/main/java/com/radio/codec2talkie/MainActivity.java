package com.radio.codec2talkie;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startBluetoothConnectActivity();
    }

    protected void startBluetoothConnectActivity() {
        Intent intent = new Intent(this, BluetoothConnectActivity.class);
        startActivity(intent);
    }
}