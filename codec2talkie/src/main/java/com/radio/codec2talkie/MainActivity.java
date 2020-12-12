package com.radio.codec2talkie;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.radio.codec2talkie.bluetooth.BluetoothConnectActivity;
import com.radio.codec2talkie.bluetooth.SocketHandler;
import com.radio.codec2talkie.usb.UsbConnectActivity;
import com.radio.codec2talkie.usb.UsbPortHandler;
import com.ustadmobile.codec2.Codec2;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private final static int REQUEST_CONNECT_BT = 1;
    private final static int REQUEST_CONNECT_USB = 2;
    private final static int REQUEST_PERMISSIONS = 3;

    private final static int CODEC2_DEFAULT_MODE = Codec2.CODEC2_MODE_450;
    private final static int CODEC2_DEFAULT_MODE_POS = 0;

    private final String[] _requiredPermissions = new String[] {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.RECORD_AUDIO
    };

    static boolean _isActive = false;

    private TextView _textConnInfo;
    private TextView _textStatus;
    private Spinner _spinnerCodec2Mode;
    private ProgressBar _progressRxLevel;
    private ProgressBar _progressTxLevel;
    private CheckBox _checkBoxLoopback;

    private Codec2Player _codec2Player;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _isActive = false;

        setContentView(R.layout.activity_main);

        _textConnInfo = findViewById(R.id.textBtName);
        _textStatus = findViewById(R.id.textStatus);
        _progressRxLevel = findViewById(R.id.progressRxLevel);
        _progressRxLevel.setMax(-Codec2Player.getAudioMinLevel());
        _progressTxLevel = findViewById(R.id.progressTxLevel);
        _progressTxLevel.setMax(-Codec2Player.getAudioMinLevel());

        Button btnPtt = findViewById(R.id.btnPtt);
        btnPtt.setOnTouchListener(onBtnPttTouchListener);

        _spinnerCodec2Mode = findViewById(R.id.spinnerCodecMode);
        _spinnerCodec2Mode.setSelection(CODEC2_DEFAULT_MODE_POS);
        _spinnerCodec2Mode.setOnItemSelectedListener(onCodecModeSelectedListener);

        _checkBoxLoopback = findViewById(R.id.checkBoxLoopback);
        _checkBoxLoopback.setOnCheckedChangeListener(onLoopbackCheckedChangeListener);

        registerReceiver(onBluetoothDisconnected, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));

        if (requestPermissions()) {
            startUsbConnectActivity();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
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

    private final CompoundButton.OnCheckedChangeListener onLoopbackCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (_codec2Player != null) {
                _codec2Player.setLoopbackMode(isChecked);
            }
        }
    };

    private final AdapterView.OnItemSelectedListener onCodecModeSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String selectedCodec = getResources().getStringArray(R.array.codec2_modes)[position];
            String [] codecNameCodecId = selectedCodec.split("=");
            if (_codec2Player != null) {
                _codec2Player.setCodecMode(Integer.parseInt(codecNameCodecId[1]));
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    private final BroadcastReceiver onBluetoothDisconnected = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        if (_codec2Player != null && SocketHandler.getSocket() != null) {
            _codec2Player.stopRunning();
        }
        }
    };

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
                _checkBoxLoopback.setChecked(false);
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
        _codec2Player = new Codec2Player(onPlayerStateChanged, CODEC2_DEFAULT_MODE);
        if (isUsb) {
            _codec2Player.setUsbPort(UsbPortHandler.getPort());
        } else {
            _codec2Player.setSocket(SocketHandler.getSocket());
        }
        _spinnerCodec2Mode.setSelection(CODEC2_DEFAULT_MODE_POS);
        _codec2Player.start();
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
        if (requestCode == REQUEST_CONNECT_USB) {
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
    }
}