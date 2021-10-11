package com.radio.codec2talkie.connect;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

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

    private final int MAX_RETRIES = 5;
    private final int RETRY_DELAY_MS = 5000;

    private final String DEFAULT_ADDRESS = "127.0.0.1";
    private final String DEFAULT_PORT = "8081";

    private String _address;
    private String _port;

    private Socket _socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tcp_ip_connect);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        _address = sharedPreferences.getString(PreferenceKeys.PORTS_TCP_IP_ADDRESS, DEFAULT_ADDRESS);
        _port = sharedPreferences.getString(PreferenceKeys.PORTS_TCP_IP_PORT, DEFAULT_PORT);

        ProgressBar progressBarTcpIp = findViewById(R.id.progressBarTcpIp);
        progressBarTcpIp.setVisibility(View.VISIBLE);
        ObjectAnimator.ofInt(progressBarTcpIp, "progress", 10)
                .setDuration(300)
                .start();
        connectSocket();
    }

    private void connectSocket() {

        new Thread() {
            @Override
            public void run() {
                Message resultMsg = new Message();
                int count = 0;
                int maxRetries = MAX_RETRIES;
                boolean connected = false;

                while (true) {
                    try {
                        _socket = new Socket();
                        _socket.connect(new InetSocketAddress(_address, Integer.parseInt(_port)));
                        connected = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                        if (++count >= maxRetries) break;
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException interruptedException) {
                            interruptedException.printStackTrace();
                        }
                    }
                    if (connected) break;
                }
                if (_socket.isConnected()) {
                    TcpIpSocketHandler.setSocket(_socket);
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
}
