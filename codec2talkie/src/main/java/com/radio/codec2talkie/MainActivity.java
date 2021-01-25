package com.radio.codec2talkie;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.radio.codec2talkie.bluetooth.BluetoothConnectActivity;
import com.radio.codec2talkie.bluetooth.SocketHandler;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.settings.SettingsActivity;
import com.radio.codec2talkie.usb.UsbConnectActivity;
import com.radio.codec2talkie.usb.UsbPortHandler;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private final static int REQUEST_CONNECT_BT = 1;
    private final static int REQUEST_CONNECT_USB = 2;
    private final static int REQUEST_PERMISSIONS = 3;
    private final static int REQUEST_SETTINGS = 4;

    private final static String CODEC2_DEFAULT_MODE = "MODE_450=10";

    private final String[] _requiredPermissions = new String[] {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.RECORD_AUDIO
    };

    SharedPreferences _sharedPreferences;

    private boolean _isActive = false;

    private TextView _textConnInfo;
    private TextView _textStatus;
    private TextView _textCodecMode;
    private ProgressBar _progressRxLevel;
    private ProgressBar _progressTxLevel;
    private Button _btnPtt;

    private Codec2Player _codec2Player;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _isActive = true;

        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_main);

        _textConnInfo = findViewById(R.id.textBtName);
        _textStatus = findViewById(R.id.textStatus);

        _progressRxLevel = findViewById(R.id.progressRxLevel);
        _progressRxLevel.setMax(-Codec2Player.getAudioMinLevel());
        _progressRxLevel.getProgressDrawable().setColorFilter(
                new PorterDuffColorFilter(colorFromAudioLevel(Codec2Player.getAudioMinLevel()), PorterDuff.Mode.SRC_IN));

        _progressTxLevel = findViewById(R.id.progressTxLevel);
        _progressTxLevel.setMax(-Codec2Player.getAudioMinLevel());
        _progressTxLevel.getProgressDrawable().setColorFilter(
                new PorterDuffColorFilter(colorFromAudioLevel(Codec2Player.getAudioMinLevel()), PorterDuff.Mode.SRC_IN));

        _btnPtt = findViewById(R.id.btnPtt);
        _btnPtt.setOnTouchListener(onBtnPttTouchListener);

        _textCodecMode = findViewById(R.id.codecMode);

        registerReceiver(onBluetoothDisconnected, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
        registerReceiver(onUsbDetached, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));

        if (requestPermissions()) {
            startUsbConnectActivity();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        _isActive = false;
        if (_codec2Player != null) {
            _codec2Player.stopRunning();
        }
    }

    protected void startUsbConnectActivity() {
        Intent usbConnectIntent = new Intent(this, UsbConnectActivity.class);
        startActivityForResult(usbConnectIntent, REQUEST_CONNECT_USB);
    }

    protected void startBluetoothConnectActivity() {
        Intent bluetoothConnectIntent = new Intent(this, BluetoothConnectActivity.class);
        startActivityForResult(bluetoothConnectIntent, REQUEST_CONNECT_BT);
    }

    protected boolean requestPermissions() {
        List<String> permissionsToRequest = new LinkedList<String>();

        for (String permission : _requiredPermissions) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED) {
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS);
            return false;
        }
        return true;
    }

    private int colorFromAudioLevel(int audioLevel) {
        int color = Color.GREEN;
        if (audioLevel > Codec2Player.getAudioHighLevel())
            color = Color.RED;
        else if (audioLevel == Codec2Player.getAudioMinLevel())
            color = Color.LTGRAY;
        return color;
    }

    private final BroadcastReceiver onBluetoothDisconnected = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        if (_codec2Player != null && SocketHandler.getSocket() != null) {
            Toast.makeText(MainActivity.this, "Bluetooth disconnected", Toast.LENGTH_SHORT).show();
            _codec2Player.stopRunning();
        }
        }
    };

    private final BroadcastReceiver onUsbDetached = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        if (_codec2Player != null && UsbPortHandler.getPort() != null) {
            Toast.makeText(MainActivity.this, "USB detached", Toast.LENGTH_SHORT).show();
            _codec2Player.stopRunning();
        }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();

        if (itemId == R.id.preferences) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivityForResult(settingsIntent, REQUEST_SETTINGS);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_TV_DATA_SERVICE:
                _btnPtt.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_TV_DATA_SERVICE:
                _btnPtt.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
                return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private final View.OnTouchListener onBtnPttTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (_codec2Player != null)
                        _codec2Player.startRecording();
                    break;
                case MotionEvent.ACTION_UP:
                    v.performClick();
                    if (_codec2Player != null)
                        _codec2Player.startPlayback();
                    break;
            }
            return false;
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                Toast.makeText(MainActivity.this, "Permissions Granted", Toast.LENGTH_SHORT).show();
                startUsbConnectActivity();
            } else {
                Toast.makeText(MainActivity.this, "Permissions Denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private final Handler onPlayerStateChanged = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (_isActive && msg.what == Codec2Player.PLAYER_DISCONNECT) {
                _textStatus.setText("STOP");
                Toast.makeText(getBaseContext(), "Disconnected from modem", Toast.LENGTH_SHORT).show();
                startUsbConnectActivity();
            }
            else if (msg.what == Codec2Player.PLAYER_LISTENING) {
                _textStatus.setText("IDLE");
            }
            else if (msg.what == Codec2Player.PLAYER_RECORDING) {
                _textStatus.setText("TX");
            }
            else if (msg.what == Codec2Player.PLAYER_PLAYING) {
                _textStatus.setText("RX");
            }
            else if (msg.what == Codec2Player.PLAYER_RX_LEVEL) {
                _progressRxLevel.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(colorFromAudioLevel(msg.arg1), PorterDuff.Mode.SRC_IN));
                _progressRxLevel.setProgress(msg.arg1 - Codec2Player.getAudioMinLevel());
            }
            else if (msg.what == Codec2Player.PLAYER_TX_LEVEL) {
                _progressTxLevel.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(colorFromAudioLevel(msg.arg1), PorterDuff.Mode.SRC_IN));
                _progressTxLevel.setProgress(msg.arg1 - Codec2Player.getAudioMinLevel());
            }
        }
    };

    private void startPlayer(boolean isUsb) throws IOException {
        String statusMessage = new String();

        String codec2ModePref = _sharedPreferences.getString(PreferenceKeys.CODEC2_MODE, CODEC2_DEFAULT_MODE);
        String [] codecNameCodecId = codec2ModePref.split("=");
        statusMessage += codecNameCodecId[0];
        int codec2Mode = Integer.parseInt(codecNameCodecId[1]);

        boolean isTestMode = _sharedPreferences.getBoolean(PreferenceKeys.CODEC2_TEST_MODE, false);
        if (isTestMode) {
            statusMessage += ", TEST";
        }

        _codec2Player = new Codec2Player(onPlayerStateChanged, codec2Mode);
        if (isUsb) {
            _codec2Player.setUsbPort(UsbPortHandler.getPort());
        } else {
            _codec2Player.setSocket(SocketHandler.getSocket());
        }
        _codec2Player.setCodecTestMode(isTestMode);
        _codec2Player.start();

        _textCodecMode.setText(statusMessage);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CONNECT_BT) {
            if (resultCode == RESULT_CANCELED) {
                finish();
            } else if (resultCode == RESULT_OK) {
                _textConnInfo.setText(data.getStringExtra("name"));
                try {
                    startPlayer(false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        else if (requestCode == REQUEST_CONNECT_USB) {
            if (resultCode == RESULT_CANCELED) {
                startBluetoothConnectActivity();
            } else if (resultCode == RESULT_OK) {
                _textConnInfo.setText(data.getStringExtra("name"));
                try {
                    startPlayer(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        else if (requestCode == REQUEST_SETTINGS) {
            finish();
            startActivity(getIntent());
        }
    }
}