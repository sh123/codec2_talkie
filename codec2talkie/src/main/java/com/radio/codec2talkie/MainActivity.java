package com.radio.codec2talkie;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.radio.codec2talkie.app.AppService;
import com.radio.codec2talkie.app.AppWorker;
import com.radio.codec2talkie.connect.BleConnectActivity;
import com.radio.codec2talkie.connect.BluetoothConnectActivity;
import com.radio.codec2talkie.connect.BluetoothSocketHandler;
import com.radio.codec2talkie.connect.TcpIpConnectActivity;
import com.radio.codec2talkie.protocol.ProtocolFactory;
import com.radio.codec2talkie.recorder.RecorderActivity;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.settings.SettingsActivity;
import com.radio.codec2talkie.tools.AudioTools;
import com.radio.codec2talkie.tools.RadioTools;
import com.radio.codec2talkie.transport.TransportFactory;
import com.radio.codec2talkie.connect.UsbConnectActivity;
import com.radio.codec2talkie.connect.UsbPortHandler;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private final static int REQUEST_CONNECT_BT = 1;
    private final static int REQUEST_CONNECT_USB = 2;
    private final static int REQUEST_PERMISSIONS = 3;
    private final static int REQUEST_SETTINGS = 4;
    private final static int REQUEST_RECORDER = 5;
    private final static int REQUEST_CONNECT_TCP_IP = 6;

    // S9 level at -93 dBm as per VHF Managers Handbook
    private final static int S_METER_S0_VALUE_DB = -147;
    // from S0 up to S9+40
    private final static int S_METER_RANGE_DB = 100;

    private final static long BACK_EXIT_MS_DELAY = 2000;

    private final String[] _requiredPermissions = new String[] {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private AppService _appService;

    private SharedPreferences _sharedPreferences;

    private boolean _isTestMode;
    private boolean _isBleEnabled;

    private TextView _textConnInfo;
    private TextView _textStatus;
    private TextView _textCodecMode;
    private TextView _textRssi;
    private ProgressBar _progressAudioLevel;
    private ProgressBar _progressRssi;
    private Button _btnPtt;

    private boolean _shouldSkipTransportReconnect = false;
    private long _backPressedTimestamp;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // title
        String appName = getResources().getString(R.string.app_name);
        setTitle(appName + " v" + BuildConfig.VERSION_NAME);

        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_main);

        _textConnInfo = findViewById(R.id.textBtName);
        _textStatus = findViewById(R.id.textStatus);
        _textRssi = findViewById(R.id.textRssi);

        // UV bar
        int barMaxValue = AppWorker.getAudioMaxLevel() - AppWorker.getAudioMinLevel();
        _progressAudioLevel = findViewById(R.id.progressAudioLevel);
        _progressAudioLevel.setMax(barMaxValue);
        _progressAudioLevel.getProgressDrawable().setColorFilter(
                new PorterDuffColorFilter(AudioTools.colorFromAudioLevel(AppWorker.getAudioMinLevel()), PorterDuff.Mode.SRC_IN));

        // S-meter
        _progressRssi = findViewById(R.id.progressRssi);
        _progressRssi.setMax(S_METER_RANGE_DB);
        _progressRssi.getProgressDrawable().setColorFilter(
                new PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN));

        // PTT button
        _btnPtt = findViewById(R.id.btnPtt);
        _btnPtt.setOnTouchListener(onBtnPttTouchListener);

        _textCodecMode = findViewById(R.id.codecMode);

        // BT/USB disconnects
        registerReceiver(onBluetoothDisconnected, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
        registerReceiver(onUsbDetached, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));

        _isTestMode = _sharedPreferences.getBoolean(PreferenceKeys.CODEC2_TEST_MODE, false);
        _isBleEnabled = _sharedPreferences.getBoolean(PreferenceKeys.PORTS_BT_BLE_ENABLED, false);

        // show/hide S-meter
        FrameLayout frameRssi = findViewById(R.id.frameRssi);
        if (_sharedPreferences.getBoolean(PreferenceKeys.KISS_EXTENSIONS_ENABLED, false)) {
            int sLevelId = RadioTools.getMinimumDecodeSLevelLabel(_sharedPreferences, S_METER_S0_VALUE_DB);
            TextView sLevel = findViewById(sLevelId);
            if (sLevel != null) {
                sLevel.setTypeface(null, Typeface.BOLD_ITALIC);
            }
        } else {
            frameRssi.setVisibility(View.GONE);
        }

        // screen always on
        if (_sharedPreferences.getBoolean(PreferenceKeys.APP_KEEP_SCREEN_ON, false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        startTransportConnection();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onBluetoothDisconnected);
        unregisterReceiver(onUsbDetached);
    }

    private void stopRunning() {
        _shouldSkipTransportReconnect = true;
        if (_appService != null) {
            _appService.stopRunning();
        }
        finish();
    }

    private void startTransportConnection() {
        if (_shouldSkipTransportReconnect) return;
        Log.i(TAG, "Starting transport connection");
        if (_isTestMode) {
            _textConnInfo.setText(R.string.main_status_loopback_test);
            startAppService(TransportFactory.TransportType.LOOPBACK);
        } else if (requestPermissions()) {
            if (_sharedPreferences.getBoolean(PreferenceKeys.PORTS_TCP_IP_ENABLED, false)) {
                startTcpIpConnectActivity();
            } else {
                startUsbConnectActivity();
            }
        }
    }

    protected void startUsbConnectActivity() {
        Intent usbConnectIntent = new Intent(this, UsbConnectActivity.class);
        startActivityForResult(usbConnectIntent, REQUEST_CONNECT_USB);
    }

    protected void startBluetoothConnectActivity() {
        Intent bluetoothConnectIntent;
        if (_isBleEnabled) {
            bluetoothConnectIntent = new Intent(this, BleConnectActivity.class);
        } else {
            bluetoothConnectIntent = new Intent(this, BluetoothConnectActivity.class);
        }
        startActivityForResult(bluetoothConnectIntent, REQUEST_CONNECT_BT);
    }

    protected void startTcpIpConnectActivity() {
        Intent tcpIpConnectIntent = new Intent(this, TcpIpConnectActivity.class);
        startActivityForResult(tcpIpConnectIntent, REQUEST_CONNECT_TCP_IP);
    }

    protected void startRecorderActivity() {
        Intent recorderIntent = new Intent(this, RecorderActivity.class);
        startActivityForResult(recorderIntent, REQUEST_RECORDER);
    }

    protected boolean requestPermissions() {
        List<String> permissionsToRequest = new LinkedList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String permission = Manifest.permission.FOREGROUND_SERVICE;
            if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED) {
                permissionsToRequest.add(permission);
            }
        }
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

    private final BroadcastReceiver onBluetoothDisconnected = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (_appService != null && BluetoothSocketHandler.getSocket() != null && !_isTestMode) {
                Toast.makeText(MainActivity.this, R.string.bt_disconnected, Toast.LENGTH_LONG).show();
                _appService.stopRunning();
            }
        }
    };

    private final BroadcastReceiver onUsbDetached = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (_appService != null && UsbPortHandler.getPort() != null && !_isTestMode) {
                Toast.makeText(MainActivity.this, R.string.usb_detached, Toast.LENGTH_LONG).show();
                _appService.stopRunning();
            }
        }
    };

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean isAprsEnabled = _sharedPreferences.getBoolean(PreferenceKeys.APRS_ENABLED, false);
        menu.setGroupVisible(R.id.group_aprs, isAprsEnabled);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuCompat.setGroupDividerEnabled(menu, true);
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
        if (itemId == R.id.recorder) {
            startRecorderActivity();
            return true;
        }
        if (itemId == R.id.reconnect) {
            if (_appService != null) {
                _appService.stopRunning();
            }
            return true;
        }
        else if (itemId == R.id.exit) {
            stopRunning();
            return true;
        }
        else if (itemId == R.id.send_position) {
            _appService.sendPosition();
            return true;
        }
        else if (itemId == R.id.start_tracking) {
            if (_appService.isTracking()) {
                _appService.stopTracking();
                item.setTitle(R.string.menu_start_tracking);
            } else {
                _appService.startTracking();
                item.setTitle(R.string.menu_stop_tracking);
            }
            return true;
        }
        else if (itemId == R.id.messages) {
            Toast.makeText(getBaseContext(), "Not implemented", Toast.LENGTH_SHORT).show();
            return true;
        }
        else if (itemId == R.id.aprs_log) {
            Toast.makeText(getBaseContext(), "Not implemented", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            // headset hardware ptt cannot be used for long press
            case KeyEvent.KEYCODE_HEADSETHOOK:
                if (_btnPtt.isPressed()) {
                    _btnPtt.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                            SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
                } else {
                    _btnPtt.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                            SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (_sharedPreferences.getBoolean(PreferenceKeys.APP_VOLUME_PTT, false)) {
                    _btnPtt.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                            SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_TV_DATA_SERVICE:
                _btnPtt.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {

        if (_backPressedTimestamp + BACK_EXIT_MS_DELAY > System.currentTimeMillis()) {
            super.onBackPressed();
            stopRunning();
        } else {
            Toast.makeText(getBaseContext(), "Press back again to exit", Toast.LENGTH_SHORT).show();
        }
        _backPressedTimestamp = System.currentTimeMillis();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (_sharedPreferences.getBoolean(PreferenceKeys.APP_VOLUME_PTT, false)) {
                    _btnPtt.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                            SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
                    return true;
                }
                break;
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
                    if (_appService != null)
                        _appService.startRecording();
                    break;
                case MotionEvent.ACTION_UP:
                    v.performClick();
                    if (_appService != null)
                        _appService.startPlayback();
                    break;
            }
            return false;
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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
                Toast.makeText(MainActivity.this, R.string.permissions_granted, Toast.LENGTH_SHORT).show();
                startUsbConnectActivity();
            } else {
                Toast.makeText(MainActivity.this, R.string.permissions_denied, Toast.LENGTH_SHORT).show();
                stopRunning();
            }
        }
    }

    private final ServiceConnection _appServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            _appService = ((AppService.AppServiceBinder)service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            _appService = null;
        }
    };

    private final Handler onAudioProcessorStateChanged = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AppWorker.PROCESSOR_CONNECTED:
                    Toast.makeText(getBaseContext(), R.string.processor_connected, Toast.LENGTH_SHORT).show();
                    bindAppService();
                    break;
                case AppWorker.PROCESSOR_DISCONNECTED:
                    _btnPtt.setText(R.string.main_status_stop);
                    Toast.makeText(getBaseContext(), R.string.processor_disconnected, Toast.LENGTH_SHORT).show();
                    unbindAppService();
                    stopAppService();
                    startTransportConnection();
                    break;
                case AppWorker.PROCESSOR_LISTENING:
                    _btnPtt.setText(R.string.push_to_talk);
                    _textStatus.setText("");
                    break;
                case AppWorker.PROCESSOR_TRANSMITTING:
                    if (msg.obj != null) {
                        _textStatus.setText((String) msg.obj);
                    }
                    _btnPtt.setText(R.string.main_status_tx);
                    break;
                case AppWorker.PROCESSOR_RECEIVING:
                    _btnPtt.setText(R.string.main_status_rx);
                    break;
                case AppWorker.PROCESSOR_PLAYING:
                    if (msg.obj != null) {
                        _textStatus.setText((String) msg.obj);
                    }
                    _btnPtt.setText(R.string.main_status_play);
                    break;
                case AppWorker.PROCESSOR_RX_RADIO_LEVEL:
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
                case AppWorker.PROCESSOR_RX_LEVEL:
                case AppWorker.PROCESSOR_TX_LEVEL:
                    _progressAudioLevel.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(AudioTools.colorFromAudioLevel(msg.arg1), PorterDuff.Mode.SRC_IN));
                    _progressAudioLevel.setProgress(msg.arg1 - AppWorker.getAudioMinLevel());
                    break;
                case AppWorker.PROCESSOR_RX_ERROR:
                    _btnPtt.setText(R.string.main_status_rx_error);
                    break;
                case AppWorker.PROCESSOR_TX_ERROR:
                    _btnPtt.setText(R.string.main_status_tx_error);
                    break;
            }
        }
    };

    private void bindAppService() {
        if (!bindService(new Intent(this, AppService.class), _appServiceConnection, Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "Service does not exists or no access");
        }
    }

    private void unbindAppService() {
        unbindService(_appServiceConnection);
    }


    private void startAppService(TransportFactory.TransportType transportType) {
        Log.i(TAG, "Starting app service processing: " + transportType.toString());

        String codec2ModeName = _sharedPreferences.getString(PreferenceKeys.CODEC2_MODE, getResources().getStringArray(R.array.codec2_modes)[0]);

        ProtocolFactory.ProtocolType protocolType = ProtocolFactory.getBaseProtocolType(getApplicationContext());
        _btnPtt.setEnabled(protocolType != ProtocolFactory.ProtocolType.KISS_PARROT);

        String statusLine = getSpeedStatusText(codec2ModeName) + ", " + getFeatureStatusText(protocolType);
        _textCodecMode.setText(statusLine);

        Intent serviceIntent = new Intent(this, AppService.class);
        serviceIntent.putExtra("transportType", transportType);
        serviceIntent.putExtra("callback", new Messenger(onAudioProcessorStateChanged));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(serviceIntent);
        else
            startService(serviceIntent);
    }

    private void stopAppService() {
        stopService(new Intent(this, AppService.class));
    }

    private String getSpeedStatusText(String codec2ModeName) {
        // codec2 speed
        String speedModeInfo = "C2: " + AudioTools.extractCodec2Speed(codec2ModeName);

        // radio speed
        int radioSpeedBps = RadioTools.getRadioSpeed(_sharedPreferences);
        if (radioSpeedBps > 0) {
            speedModeInfo = "RF: " + radioSpeedBps + ", " + speedModeInfo;
        }
        return speedModeInfo;
    }

    private String getFeatureStatusText(ProtocolFactory.ProtocolType protocolType) {
        // protocol
        String status = "";

        // recording
        boolean recordingEnabled = _sharedPreferences.getBoolean(PreferenceKeys.CODEC2_RECORDING_ENABLED, false);
        if (recordingEnabled) {
            status += getString(R.string.recorder_status_label);
        }

        // scrambling
        boolean scramblingEnabled = _sharedPreferences.getBoolean(PreferenceKeys.KISS_SCRAMBLING_ENABLED, false);
        if (scramblingEnabled) {
            status += getString(R.string.kiss_scrambler_label);
        }

        // aprs
        boolean aprsEnabled = _sharedPreferences.getBoolean(PreferenceKeys.APRS_ENABLED, false);
        if (aprsEnabled) {
            status += getString(R.string.aprs_label);

            // VoAX25
            boolean voax25Enabled = _sharedPreferences.getBoolean(PreferenceKeys.APRS_VOAX25_ENABLE, false);
            if (voax25Enabled) {
                status += getString(R.string.voax25_label);
            }
        }

        if (status.length() == 0) {
            return protocolType.toString();
        }
        return protocolType.toString() + " " + status;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CONNECT_BT) {
            if (resultCode == RESULT_CANCELED) {
                // fall back to loopback if bluetooth failed
                _textConnInfo.setText(R.string.main_status_loopback_test);
                startAppService(TransportFactory.TransportType.LOOPBACK);
            } else if (resultCode == RESULT_OK) {
                _textConnInfo.setText(data.getStringExtra("name"));
                if (_isBleEnabled) {
                    startAppService(TransportFactory.TransportType.BLE);
                } else {
                    startAppService(TransportFactory.TransportType.BLUETOOTH);
                }
            }
        }
        else if (requestCode == REQUEST_CONNECT_TCP_IP) {
            if (resultCode == RESULT_CANCELED) {
                // fall back to loopback if tcp/ip failed
                _textConnInfo.setText(R.string.main_status_loopback_test);
                startAppService(TransportFactory.TransportType.LOOPBACK);
            } else if (resultCode == RESULT_OK) {
                _textConnInfo.setText(data.getStringExtra("name"));
                startAppService(TransportFactory.TransportType.TCP_IP);
            }
        }
        else if (requestCode == REQUEST_CONNECT_USB) {
            if (resultCode == RESULT_CANCELED) {
                // fall back to bluetooth if usb failed
                startBluetoothConnectActivity();
            } else if (resultCode == RESULT_OK) {
                _textConnInfo.setText(data.getStringExtra("name"));
                startAppService(TransportFactory.TransportType.USB);
            }
        }
        else if (requestCode == REQUEST_SETTINGS) {
            _shouldSkipTransportReconnect = true;
            stopRunning();
            startActivity(getIntent());
        }
    }
}