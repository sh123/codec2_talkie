package com.radio.codec2talkie;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.radio.codec2talkie.app.AppMessage;
import com.radio.codec2talkie.app.AppService;
import com.radio.codec2talkie.app.AppWorker;
import com.radio.codec2talkie.connect.BleConnectActivity;
import com.radio.codec2talkie.connect.BluetoothConnectActivity;
import com.radio.codec2talkie.connect.BluetoothSocketHandler;
import com.radio.codec2talkie.connect.TcpIpConnectActivity;
import com.radio.codec2talkie.maps.MapActivity;
import com.radio.codec2talkie.settings.SettingsWrapper;
import com.radio.codec2talkie.storage.log.LogItemActivity;
import com.radio.codec2talkie.protocol.ProtocolFactory;
import com.radio.codec2talkie.recorder.RecorderActivity;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.settings.SettingsActivity;
import com.radio.codec2talkie.storage.message.group.MessageGroupActivity;
import com.radio.codec2talkie.tools.AudioTools;
import com.radio.codec2talkie.tools.DeviceIdTools;
import com.radio.codec2talkie.tools.RadioTools;
import com.radio.codec2talkie.transport.TransportFactory;
import com.radio.codec2talkie.connect.UsbConnectActivity;
import com.radio.codec2talkie.connect.UsbPortHandler;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ServiceConnection {

    private static final String TAG = MainActivity.class.getSimpleName();

    private final static int REQUEST_PERMISSIONS = 1;

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
    private TextView _textTelemetry;
    private TextView _textCodecMode;
    private TextView _textRssi;
    private ProgressBar _progressAudioLevel;
    private ProgressBar _progressRssi;
    private ImageButton _btnPtt;
    private Menu _menu;

    public static boolean isPaused = false;
    private boolean _isConnecting = false;
    private boolean _isAppExit = false;
    private boolean _isAppRestart = false;
    private boolean _isRigCtlUsbConnected = false;

    private long _backPressedTimestamp;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        // title
        String appName = getResources().getString(R.string.app_name);
        setTitle(appName + " v" + BuildConfig.VERSION_NAME);

        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_main);

        _textConnInfo = findViewById(R.id.textBtName);
        _textStatus = findViewById(R.id.textStatus);
        _textTelemetry = findViewById(R.id.textTelemetry);
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
        _btnPtt.setEnabled(false);
        _btnPtt.setOnTouchListener(onBtnPttTouchListener);

        _textCodecMode = findViewById(R.id.codecMode);

        // BT/USB disconnects
        registerReceiver(onBluetoothDisconnected, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
        registerReceiver(onUsbDetached, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));

        _isTestMode = SettingsWrapper.isLoopbackTransport(_sharedPreferences);
        _isBleEnabled = SettingsWrapper.isBleTransport(_sharedPreferences);

        // show/hide S-meter
        FrameLayout frameRssi = findViewById(R.id.frameRssi);
        if (SettingsWrapper.isFreeDvSoundModemModulation(_sharedPreferences)) {
            frameRssi.setVisibility(View.VISIBLE);
        } else if (SettingsWrapper.isKissExtensionEnabled(_sharedPreferences) && !SettingsWrapper.isSoundModemEnabled(_sharedPreferences)) {
            int sLevelId = RadioTools.getMinimumDecodeSLevelLabel(_sharedPreferences, S_METER_S0_VALUE_DB);
            TextView sLevel = findViewById(sLevelId);
            if (sLevel != null) {
                sLevel.setTypeface(null, Typeface.BOLD_ITALIC);
            }
            frameRssi.setVisibility(View.VISIBLE);
        } else {
            frameRssi.setVisibility(View.GONE);
        }

        // screen always on / auto turn screen on / run above app lock
        if (_sharedPreferences.getBoolean(PreferenceKeys.APP_KEEP_SCREEN_ON, false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        if (_sharedPreferences.getBoolean(PreferenceKeys.APP_TURN_SCREEN_ON, false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        if (_sharedPreferences.getBoolean(PreferenceKeys.APP_NO_LOCK, false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }

        _isAppRestart = false;
        _isAppExit = false;

        // load device id description mapping
        DeviceIdTools.loadDeviceIdMap(this);

        startTransportConnection();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop()");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, "onRestart()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");
        unregisterReceiver(onBluetoothDisconnected);
        unregisterReceiver(onUsbDetached);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");
        isPaused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");
        isPaused = false;
        if (!AppService.isRunning && !_isConnecting) {
            _btnPtt.setEnabled(false);
            startTransportConnection();
        }
    }

    private void exitApplication() {
        Log.i(TAG, "exitApplication()");
        _isAppExit = true;
        if (_appService != null) {
            _appService.stopRunning();
        }
    }

    private void restartApplication() {
        Log.i(TAG, "restartApplication()");
        _isAppRestart = true;
        if (_appService != null) {
            _appService.stopRunning();
        }
    }

    private void startTransportConnection() {
        Log.i(TAG, "startTransportConnection()");
        if (AppService.isRunning) {
            startAppService(AppService.transportType);
        } else if (requestPermissions()) {
            switch (SettingsWrapper.getCurrentTransportType(_sharedPreferences)) {
                case LOOPBACK:
                    _textConnInfo.setText(R.string.main_status_loopback_test);
                    startAppService(TransportFactory.TransportType.LOOPBACK);
                    break;
                case SOUND_MODEM:
                    _textConnInfo.setText(R.string.main_status_sound_modem);
                    // start sound modem without rig cat usb connection
                    if (SettingsWrapper.isSoundModemRigDisabled(_sharedPreferences))
                        startAppService(TransportFactory.TransportType.SOUND_MODEM);
                    // otherwise try to connect with usb for cat ptt
                    else
                        startUsbConnectActivity();
                    break;
                case TCP_IP:
                    startTcpIpConnectActivity();
                    break;
                case USB:
                    startUsbConnectActivity();
                    break;
                case BLUETOOTH:
                case BLE:
                    startBluetoothConnectActivity();
                    break;
            }
        }
    }

    private final ActivityResultLauncher<Intent> _usbActivityLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            int resultCode = result.getResultCode();
            if (SettingsWrapper.isSoundModemEnabled(_sharedPreferences)) {
                _isRigCtlUsbConnected = resultCode == RESULT_OK;
                startAppService(TransportFactory.TransportType.SOUND_MODEM);
            } else if (resultCode == RESULT_CANCELED) {
                _textConnInfo.setText(R.string.main_status_loopback_test);
                startAppService(TransportFactory.TransportType.LOOPBACK);
            } else if (resultCode == RESULT_OK) {
                Intent data = result.getData();
                assert data != null;
                _textConnInfo.setText(data.getStringExtra("name"));
                startAppService(TransportFactory.TransportType.USB);
            }
        }
    );

    protected void startUsbConnectActivity() {
        _isConnecting = true;
        _isRigCtlUsbConnected = false;
        _usbActivityLauncher.launch(new Intent(this, UsbConnectActivity.class));
    }

    private final ActivityResultLauncher<Intent> _bluetoothActivityLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            int resultCode = result.getResultCode();
            if (resultCode == RESULT_CANCELED) {
                // fall back to loopback if bluetooth failed
                _textConnInfo.setText(R.string.main_status_loopback_test);
                startAppService(TransportFactory.TransportType.LOOPBACK);
            } else if (resultCode == RESULT_OK) {
                Intent data = result.getData();
                assert data != null;
                _textConnInfo.setText(data.getStringExtra("name"));
                if (_isBleEnabled) {
                    startAppService(TransportFactory.TransportType.BLE);
                } else {
                    startAppService(TransportFactory.TransportType.BLUETOOTH);
                }
            }
        }
    );

    protected void startBluetoothConnectActivity() {
        _isConnecting = true;
        Intent bluetoothConnectIntent;
        if (_isBleEnabled) {
            bluetoothConnectIntent = new Intent(this, BleConnectActivity.class);
        } else {
            bluetoothConnectIntent = new Intent(this, BluetoothConnectActivity.class);
        }
        _bluetoothActivityLauncher.launch(bluetoothConnectIntent);
    }

    private final ActivityResultLauncher<Intent> _tcpipActivityLauncher  = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            int resultCode = result.getResultCode();
            if (resultCode == RESULT_CANCELED) {
                // fall back to loopback if tcp/ip failed
                _textConnInfo.setText(R.string.main_status_loopback_test);
                startAppService(TransportFactory.TransportType.LOOPBACK);
            } else if (resultCode == RESULT_OK) {
                Intent data = result.getData();
                assert data != null;
                _textConnInfo.setText(data.getStringExtra("name"));
                startAppService(TransportFactory.TransportType.TCP_IP);
            }
        }
    );

    protected void startTcpIpConnectActivity() {
        _isConnecting = true;
        _tcpipActivityLauncher.launch(new Intent(this, TcpIpConnectActivity.class));
    }

    private final ActivityResultLauncher<Intent> _recorderActivityLauncher  = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> { });

    protected void startRecorderActivity() {
        _recorderActivityLauncher.launch(new Intent(this, RecorderActivity.class));
    }

    private final ActivityResultLauncher<Intent> _logViewActivityLauncher  = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> { });

    protected void startLogViewActivity() {
        _logViewActivityLauncher.launch(new Intent(this, LogItemActivity.class));
    }

    protected void startMapViewActivity() {
        _logViewActivityLauncher.launch(new Intent(this, MapActivity.class));
    }
    private final ActivityResultLauncher<Intent> _settingsActivityLauncher  = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(), result -> restartApplication());

    protected void startSettingsActivity() {
        _settingsActivityLauncher.launch(new Intent(this, SettingsActivity.class));
    }

    private final ActivityResultLauncher<Intent> _messagesActivityLauncher  = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {});

    protected void startMessagesActivity() {
        _messagesActivityLauncher.launch(new Intent(this, MessageGroupActivity.class));
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
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS);
            return false;
        }
        return true;
    }

    private void bindAppService() {
        if (!bindService(new Intent(this, AppService.class), this, Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "Service does not exists or no access");
        }
    }

    private void unbindAppService() {
        unbindService(this);
    }

    private void startAppService(TransportFactory.TransportType transportType) {
        Log.i(TAG, "Starting app service processing: " + transportType.toString());

        ProtocolFactory.ProtocolType protocolType = ProtocolFactory.getBaseProtocolType(getApplicationContext());
        _btnPtt.setEnabled(protocolType != ProtocolFactory.ProtocolType.KISS_PARROT);

        updateStatusText(protocolType);

        Intent serviceIntent = new Intent(this, AppService.class);
        serviceIntent.putExtra("transportType", transportType);
        serviceIntent.putExtra("callback", new Messenger(onAppServiceStateChanged));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(serviceIntent);
        else
            startService(serviceIntent);

        bindAppService();
    }

    private void stopAppService() {
        stopService(new Intent(this, AppService.class));
    }

    private void updateStatusText(ProtocolFactory.ProtocolType protocolType) {
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

        // rig CAT control
        String rigName = _sharedPreferences.getString(PreferenceKeys.PORTS_SOUND_MODEM_RIG, "Disabled");
        if (!rigName.equals("Disabled") && _isRigCtlUsbConnected) {
            status += getString(R.string.ports_sound_modem_rig_label);
        }

        // aprs
        boolean aprsEnabled = SettingsWrapper.isAprsEnabled(_sharedPreferences);
        if (aprsEnabled) {
            status += getString(R.string.aprs_label);

            // VoAX25
            boolean voax25Enabled = SettingsWrapper.isVoax25Enabled(_sharedPreferences);
            if (voax25Enabled) {
                status += getString(R.string.voax25_label);
            }

            // Lora aprs text packets
            boolean textPacketsEnabled = SettingsWrapper.isTextPacketsEnabled(_sharedPreferences);
            if (textPacketsEnabled) {
                status += getString(R.string.text_packets_label);
            }
            // Digirepeater
            boolean isDigirepeaterEnabled = _sharedPreferences.getBoolean(PreferenceKeys.AX25_DIGIREPEATER_ENABLED, false);
            if (isDigirepeaterEnabled) {
                status += getString(R.string.digirepeater_label);
            }

            // APRSIS
            boolean aprsisEnabled = SettingsWrapper.isAprsIsEnabled(_sharedPreferences);
            if (aprsisEnabled) {
                status += getString(R.string.aprsis_label);
            }
        }

        if (_appService != null) {
            boolean isTracking = _appService.isTracking();
            if (isTracking) {
                status += getString(R.string.tracking_label);
            }
        }

        status = status.isEmpty() ? protocolType.toString() : protocolType.toString() + " " + status;

        String statusLine = AudioTools.getSpeedStatusText(_sharedPreferences, getResources()) + ", " + status;
        _textCodecMode.setText(statusLine);
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
        boolean isAprsEnabled = SettingsWrapper.isAprsEnabled(_sharedPreferences);
        menu.setGroupVisible(R.id.group_aprs, isAprsEnabled);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuCompat.setGroupDividerEnabled(menu, true);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        _menu = menu;
        updateMenuItemsAndStatusText();
        return true;
    }

    private void updateMenuItemsAndStatusText() {
        if (AppService.isRunning & _menu != null && _appService != null) {
            MenuItem item = _menu.findItem(R.id.start_tracking);
            if (_appService.isTracking()) {
                item.setTitle(R.string.menu_stop_tracking);
            } else {
                item.setTitle(R.string.menu_start_tracking);
            }
            updateStatusText(ProtocolFactory.getBaseProtocolType(getApplicationContext()));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();

        if (itemId == R.id.preferences) {
            if (_appService != null && _appService.isTracking())
                _appService.stopTracking();
            startSettingsActivity();
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
            exitApplication();
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
            startMessagesActivity();
            return true;
        }
        else if (itemId == R.id.aprs_log) {
            startLogViewActivity();
            return true;
        }
        else if (itemId == R.id.aprs_map) {
            startMapViewActivity();
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
            //super.onBackPressed();
            exitApplication();
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
                        _appService.startTransmit();
                    break;
                case MotionEvent.ACTION_UP:
                    v.performClick();
                    if (_appService != null)
                        _appService.startReceive();
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
            } else {
                Toast.makeText(MainActivity.this, R.string.permissions_denied, Toast.LENGTH_SHORT).show();
                exitApplication();
            }
        }
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        Log.i(TAG, "Connected to app service");
        _appService = ((AppService.AppServiceBinder)service).getService();
        if (AppService.isRunning) {
            _textConnInfo.setText(_appService.getTransportName());
            updateMenuItemsAndStatusText();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        Log.i(TAG, "Disconnected from app service");
        _appService = null;
    }

    private final Handler onAppServiceStateChanged = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (AppMessage.values()[msg.what]) {
                case EV_CONNECTED:
                    Log.i(TAG, "EV_CONNECTED");
                    updateMenuItemsAndStatusText();
                    _isConnecting = false;
                    _btnPtt.setEnabled(true);
                    Toast.makeText(getBaseContext(), R.string.processor_connected, Toast.LENGTH_SHORT).show();
                    break;
                case EV_DISCONNECTED:
                    Log.i(TAG, "EV_DISCONNECTED");
                    updateMenuItemsAndStatusText();
                    _btnPtt.setImageResource(R.drawable.btn_ptt_stop);
                    _btnPtt.setEnabled(false);
                    // app restart, stop app service and restart ourselves
                    if (_isAppRestart) {
                        Log.i(TAG, "App restart");
                        unbindAppService();
                        stopAppService();
                        finish();
                        startActivity(getIntent());
                    // app exit, stop app service and finish
                    } else if (_isAppExit) {
                        Log.i(TAG, "App exit");
                        unbindAppService();
                        stopAppService();
                        finish();
                    // otherwise just reconnect if app is not on pause
                    } else if (!isPaused) {
                        Log.i(TAG, "App restart transport");
                        Toast.makeText(getBaseContext(), R.string.processor_disconnected, Toast.LENGTH_SHORT).show();
                        startTransportConnection();
                    }
                    break;
                case EV_LISTENING:
                    _btnPtt.setImageResource(R.drawable.btn_ptt_touch);
                    _textStatus.setText("");
                    break;
                case EV_TRANSMITTED_VOICE:
                    if (msg.obj != null) {
                        _textStatus.setText((String) msg.obj);
                    }
                    _btnPtt.setImageResource(R.drawable.btn_ptt_mic);
                    break;
                case EV_RECEIVING:
                    _btnPtt.setImageResource(R.drawable.btn_ptt_listen);
                    break;
                case EV_TEXT_MESSAGE_RECEIVED:
                case EV_DATA_RECEIVED:
                case EV_POSITION_RECEIVED:
                    if (msg.obj != null) {
                        String note = (String)msg.obj;
                        _textStatus.setText(note.split(":")[0]);
                    }
                    _btnPtt.setImageResource(R.drawable.btn_ptt_letter);
                    break;
                case EV_VOICE_RECEIVED:
                    if (msg.obj != null) {
                        _textStatus.setText((String) msg.obj);
                    }
                    //_btnPtt.setText(R.string.main_status_voice_received);
                    _btnPtt.setImageResource(R.drawable.btn_ptt_listen);
                    break;
                case EV_RX_RADIO_LEVEL:
                    if (msg.arg1 == 0 && msg.arg2 == 0) {
                        _textRssi.setText("");
                        _progressRssi.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN));
                        _progressRssi.setProgress(0);
                    } else if (msg.arg1 == 0) {
                        double snr = (double)msg.arg2 / 100.0;
                        _textRssi.setText(String.format(Locale.getDefault(), "%2.2f", (double)msg.arg2 / 100.0));
                        _progressRssi.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN));
                        _progressRssi.setProgress((int) ((-110.0 + snr) - S_METER_S0_VALUE_DB));
                    } else {
                        _textRssi.setText(String.format(Locale.getDefault(), "%3d dBm, %2.2f", msg.arg1, (double)msg.arg2 / 100.0));
                        _progressRssi.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN));
                        _progressRssi.setProgress(msg.arg1 - S_METER_S0_VALUE_DB);
                    }
                    break;
                case EV_TELEMETRY:
                    if (msg.arg1 > 0) {
                        // NOTE, reuse status indicator for voltage
                        _textTelemetry.setText(String.format(Locale.getDefault(), "%2.2fV", (double)msg.arg1 / 100.0));
                    }
                    break;
                // same progress bar is reused for rx and tx levels
                case EV_RX_LEVEL:
                case EV_TX_LEVEL:
                    _progressAudioLevel.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(AudioTools.colorFromAudioLevel(msg.arg1), PorterDuff.Mode.SRC_IN));
                    _progressAudioLevel.setProgress(msg.arg1 - AppWorker.getAudioMinLevel());
                    break;
                case EV_RX_ERROR:
                    _btnPtt.setImageResource(R.drawable.btn_ptt_err_rx);
                    break;
                case EV_TX_ERROR:
                    _btnPtt.setImageResource(R.drawable.btn_ptt_err_tx);
                    break;
                case EV_STARTED_TRACKING:
                case EV_STOPPED_TRACKING:
                    updateStatusText(ProtocolFactory.getBaseProtocolType(getApplicationContext()));
                    break;
            }
        }
    };
}