package com.radio.codec2talkie;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private final static int REQUEST_CONNECT_BT = 1;

    private TextView _textBtName;

    private Codec2Player _codec2Player;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startBluetoothConnectActivity();

        _textBtName = (TextView)findViewById(R.id.textBtName);
        Button _btnPtt = (Button) findViewById(R.id.btnPtt);
        _btnPtt.setOnTouchListener(onBtnPttTouchListener);
    }

    protected void startBluetoothConnectActivity() {
        Intent bluetoothConnectIntent = new Intent(this, BluetoothConnectActivity.class);
        startActivityForResult(bluetoothConnectIntent, REQUEST_CONNECT_BT);
    }

    private final View.OnTouchListener onBtnPttTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Start streaming from mic
                    break;
                case MotionEvent.ACTION_UP:
                    v.performClick();
                    // Start receiving from bluetooth
                    break;
            }
            return false;
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CONNECT_BT) {
            if (resultCode == RESULT_CANCELED) {
                finish();
            } else if (resultCode == RESULT_OK) {
                _textBtName.setText(data.getStringExtra("name"));
                try {
                    _codec2Player = new Codec2Player(BluetoothSocketHandler.getSocket());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}