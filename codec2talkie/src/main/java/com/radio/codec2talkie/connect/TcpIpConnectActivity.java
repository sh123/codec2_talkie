package com.radio.codec2talkie.connect;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.settings.PreferenceKeys;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TcpIpConnectActivity extends AppCompatActivity {

    private final int TCP_IP_CONNECTED = 1;
    private final int TCP_IP_FAILED = 2;

    private final int DEFAULT_MAX_RETRIES = 5;
    private final int DEFAULT_RETRY_DELAY_MS = 5000;

    private final String DEFAULT_ADDRESS = "127.0.0.1";
    private final String DEFAULT_PORT = "8081";

    private String _address;
    private String _port;
    private int _maxRetries;
    private int _retryDelayMs;

    private Socket _socket;

    private Boolean _cancel = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tcp_ip_connect);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        _address = sharedPreferences.getString(PreferenceKeys.PORTS_TCP_IP_ADDRESS, DEFAULT_ADDRESS);
        _port = sharedPreferences.getString(PreferenceKeys.PORTS_TCP_IP_PORT, DEFAULT_PORT);
        _maxRetries = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.PORTS_TCP_IP_RETRY_COUNT, String.valueOf(DEFAULT_MAX_RETRIES)));;
        _retryDelayMs = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.PORTS_TCP_IP_RETRY_DELAY, String.valueOf(DEFAULT_RETRY_DELAY_MS)));

        ProgressBar progressBarTcpIp = findViewById(R.id.progressBarTcpIp);
        progressBarTcpIp.setVisibility(View.VISIBLE);
        ObjectAnimator.ofInt(progressBarTcpIp, "progress", 10)
                .setDuration(300)
                .start();

        connectSocket();
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

    private void connectSocket() {

        new Thread() {
            @Override
            public void run() {

                Message resultMsg = new Message();

                int count = 0;
                boolean connected = false;

                while (!_cancel) {
                    try {
                        _socket = new Socket();
                        _socket.connect(new InetSocketAddress(_address, Integer.parseInt(_port)));
                        connected = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                        if (++count >= _maxRetries || _cancel) break;
                        try {
                            Thread.sleep(_retryDelayMs);
                        } catch (InterruptedException interruptedException) {
                            interruptedException.printStackTrace();
                        }
                    }
                    if (connected) break;
                }
                if (_socket.isConnected()) {
                    TcpIpSocketHandler.setSocket(_socket);
                    TcpIpSocketHandler.setName(String.format("%s:%s", _address, _port));
                    resultMsg.what = TCP_IP_CONNECTED;
                } else {
                    resultMsg.what = TCP_IP_FAILED;
                }
                onTcpIpStateChanged.sendMessage(resultMsg);
            }
        }.start();
    }

    private final Handler onTcpIpStateChanged = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            String toastMsg;
            if (msg.what == TCP_IP_FAILED) {
                toastMsg = getString(R.string.tcp_ip_connection_failed, _address, _port);
            } else  {
                toastMsg = getString(R.string.tcp_ip_connected, _address, _port);

                Intent resultIntent = new Intent();
                resultIntent.putExtra("name", String.format("%s:%s", _address, _port));
                setResult(Activity.RESULT_OK, resultIntent);
            }
            Toast.makeText(getBaseContext(), toastMsg, Toast.LENGTH_SHORT).show();
            finish();
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK  && event.getRepeatCount() == 0) {
            _cancel = true;
        }
        return super.onKeyDown(keyCode, event);
    }

}
