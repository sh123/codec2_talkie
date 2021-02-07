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
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.radio.codec2talkie.connect.BluetoothConnectActivity;
import com.radio.codec2talkie.connect.SocketHandler;
import com.radio.codec2talkie.protocol.ProtocolFactory;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.settings.SettingsActivity;
import com.radio.codec2talkie.transport.TransportFactory;
import com.radio.codec2talkie.connect.UsbConnectActivity;
import com.radio.codec2talkie.connect.UsbPortHandler;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private final static int REQUEST_CONNECT_BT = 1;
    private final static int REQUEST_CONNECT_USB = 2;
    private final static int REQUEST_PERMISSIONS = 3;
    private final static int REQUEST_SETTINGS = 4;

    private final static int S_METER_S0_VALUE_DB = -153;
    private final static int S_METER_RANGE_DB = 100;

    private final static int UV_METER_MIN_DELTA = 30;
    private final static int UV_METER_MAX_DELTA = -10;

    private final String[] _requiredPermissions = new String[] {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.RECORD_AUDIO
    };

    private AudioProcessor _audioProcessor;

    private SharedPreferences _sharedPreferences;

    private boolean _isTestMode;

    private TextView _textConnInfo;
    private TextView _textStatus;
    private TextView _textCodecMode;
    private TextView _textRssi;
    private ProgressBar _progressAudioLevel;
    private ProgressBar _progressRssi;
    private Button _btnPtt;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String appName = getResources().getString(R.string.app_name);
        setTitle(appName + " v" + BuildConfig.VERSION_NAME);

        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_main);

        _textConnInfo = findViewById(R.id.textBtName);
        _textStatus = findViewById(R.id.textStatus);
        _textRssi = findViewById(R.id.textRssi);

        int barMaxValue = AudioProcessor.getAudioMaxLevel() - AudioProcessor.getAudioMinLevel();
        _progressAudioLevel = findViewById(R.id.progressAudioLevel);
        _progressAudioLevel.setMax(barMaxValue);
        _progressAudioLevel.getProgressDrawable().setColorFilter(
                new PorterDuffColorFilter(colorFromAudioLevel(AudioProcessor.getAudioMinLevel()), PorterDuff.Mode.SRC_IN));

        _progressRssi = findViewById(R.id.progressRssi);
        _progressRssi.setMax(S_METER_RANGE_DB);
        _progressRssi.getProgressDrawable().setColorFilter(
                new PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN));

        _btnPtt = findViewById(R.id.btnPtt);
        _btnPtt.setOnTouchListener(onBtnPttTouchListener);

        _textCodecMode = findViewById(R.id.codecMode);

        registerReceiver(onBluetoothDisconnected, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
        registerReceiver(onUsbDetached, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));

        _isTestMode = _sharedPreferences.getBoolean(PreferenceKeys.CODEC2_TEST_MODE, false);

        FrameLayout frameRssi = findViewById(R.id.frameRssi);
        if (!_sharedPreferences.getBoolean(PreferenceKeys.KISS_EXTENSIONS_ENABLED, false)) {
            frameRssi.setVisibility(View.GONE);
        }
        startTransportConnection();
    }

    @Override
    protected void onDestroy() {
        if (_audioProcessor != null) {
            _audioProcessor.stopRunning();
        }
        super.onDestroy();
    }

    private void startTransportConnection() {
        if (_isTestMode) {
            _textConnInfo.setText(R.string.main_status_loopback_test);
            startAudioProcessing(TransportFactory.TransportType.LOOPBACK);
        } else if (requestPermissions()) {
            startUsbConnectActivity();
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
        if (audioLevel > AudioProcessor.getAudioMaxLevel() + UV_METER_MAX_DELTA)
            color = Color.RED;
        else if (audioLevel < AudioProcessor.getAudioMinLevel() + UV_METER_MIN_DELTA)
            color = Color.LTGRAY;
        return color;
    }

    private final BroadcastReceiver onBluetoothDisconnected = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        if (_audioProcessor != null && SocketHandler.getSocket() != null && !_isTestMode) {
            Toast.makeText(MainActivity.this, "Bluetooth disconnected", Toast.LENGTH_LONG).show();
            _audioProcessor.stopRunning();
        }
        }
    };

    private final BroadcastReceiver onUsbDetached = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        if (_audioProcessor != null && UsbPortHandler.getPort() != null && !_isTestMode) {
            Toast.makeText(MainActivity.this, "USB detached", Toast.LENGTH_LONG).show();
            _audioProcessor.stopRunning();
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
        else if (itemId == R.id.exit) {
            finish();
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
                    if (_audioProcessor != null)
                        _audioProcessor.startRecording();
                    break;
                case MotionEvent.ACTION_UP:
                    v.performClick();
                    if (_audioProcessor != null)
                        _audioProcessor.startPlayback();
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

    private final Handler onAudioProcessorStateChanged = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AudioProcessor.PROCESSOR_CONNECTED:
                    Toast.makeText(getBaseContext(), "Connected", Toast.LENGTH_SHORT).show();
                    break;
                case AudioProcessor.PROCESSOR_DISCONNECTED:
                    _textStatus.setText(R.string.main_status_stop);
                    Toast.makeText(getBaseContext(), "Disconnected", Toast.LENGTH_SHORT).show();
                    startTransportConnection();
                    break;
                case AudioProcessor.PROCESSOR_LISTENING:
                    _textStatus.setText(R.string.main_status_idle);
                    break;
                case AudioProcessor.PROCESSOR_RECORDING:
                    _textStatus.setText(R.string.main_status_tx);
                    break;
                case AudioProcessor.PROCESSOR_RECEIVING:
                    _textStatus.setText(R.string.main_status_rx);
                    break;
                case AudioProcessor.PROCESSOR_PLAYING:
                    _textStatus.setText(R.string.main_status_play);
                    break;
                case AudioProcessor.PROCESSOR_RX_RADIO_LEVEL:
                    if (msg.arg1 == 0) {
                        _textRssi.setText("");
                        _progressRssi.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN));
                        _progressRssi.setProgress(0);
                    } else {
                        _textRssi.setText(String.format(Locale.getDefault(), "%3d dBm, %2.2f", msg.arg1, (double)msg.arg2 / 100.0));
                        _progressRssi.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN));
                        _progressRssi.setProgress(msg.arg1 - S_METER_S0_VALUE_DB);
                    }
                    break;
                // same progress bar is reused for rx and tx levels
                case AudioProcessor.PROCESSOR_RX_LEVEL:
                case AudioProcessor.PROCESSOR_TX_LEVEL:
                    _progressAudioLevel.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(colorFromAudioLevel(msg.arg1), PorterDuff.Mode.SRC_IN));
                    _progressAudioLevel.setProgress(msg.arg1 - AudioProcessor.getAudioMinLevel());
                    break;
                case AudioProcessor.PROCESSOR_CODEC_ERROR:
                    _textStatus.setText(R.string.main_status_codec_error);
                    break;
            }
        }
    };

    private ProtocolFactory.ProtocolType getRequiredProtocolType() {
        ProtocolFactory.ProtocolType protocolType;

        if (_sharedPreferences.getBoolean(PreferenceKeys.KISS_ENABLED, true)) {
            if (_sharedPreferences.getBoolean(PreferenceKeys.KISS_PARROT, false)) {
                protocolType = ProtocolFactory.ProtocolType.KISS_PARROT;
            }
            else if (_sharedPreferences.getBoolean(PreferenceKeys.KISS_BUFFERED_ENABLED, false)) {
                protocolType = ProtocolFactory.ProtocolType.KISS_BUFFERED;
            }
            else {
                protocolType = ProtocolFactory.ProtocolType.KISS;
            }
        } else {
            protocolType = ProtocolFactory.ProtocolType.RAW;
        }
        return protocolType;
    }

    private void startAudioProcessing(TransportFactory.TransportType transportType) {
        try {
            // code mode
            String codec2ModeName = _sharedPreferences.getString(PreferenceKeys.CODEC2_MODE, getResources().getStringArray(R.array.codec2_modes)[0]);
            String[] codecNameCodecId = codec2ModeName.split("=");
            String codecMode = codecNameCodecId[0];
            int codec2ModeId = Integer.parseInt(codecNameCodecId[1]);

            ProtocolFactory.ProtocolType protocolType = getRequiredProtocolType();
            _btnPtt.setEnabled(protocolType != ProtocolFactory.ProtocolType.KISS_PARROT);

            codecMode += ", " + protocolType.toString();
            _textCodecMode.setText(codecMode);

            _audioProcessor = new AudioProcessor(transportType, protocolType, codec2ModeId, onAudioProcessorStateChanged, getApplicationContext());
            _audioProcessor.start();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Failed to start audio processing", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CONNECT_BT) {
            if (resultCode == RESULT_CANCELED) {
                // fall back to loopback if bluetooth failed
                _textConnInfo.setText(R.string.main_status_loopback_test);
                startAudioProcessing(TransportFactory.TransportType.LOOPBACK);
            } else if (resultCode == RESULT_OK) {
                _textConnInfo.setText(data.getStringExtra("name"));
                startAudioProcessing(TransportFactory.TransportType.BLUETOOTH);
            }
        }
        else if (requestCode == REQUEST_CONNECT_USB) {
            if (resultCode == RESULT_CANCELED) {
                // fall back to bluetooth if usb failed
                startBluetoothConnectActivity();
            } else if (resultCode == RESULT_OK) {
                _textConnInfo.setText(data.getStringExtra("name"));
                startAudioProcessing(TransportFactory.TransportType.USB);
            }
        }
        else if (requestCode == REQUEST_SETTINGS) {
            finish();
            startActivity(getIntent());
        }
    }
}