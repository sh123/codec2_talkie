package com.radio.codec2talkie.connect;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.Ch34xSerialDriver;
import com.hoho.android.usbserial.driver.Cp21xxSerialDriver;
import com.hoho.android.usbserial.driver.FtdiSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.radio.codec2talkie.R;
import com.radio.codec2talkie.settings.PreferenceKeys;

import java.io.IOException;
import java.util.List;

public class UsbConnectActivity extends AppCompatActivity {
    private static final String TAG = UsbConnectActivity.class.getSimpleName();

    private final int USB_NOT_FOUND = 1;
    private final int USB_CONNECTED = 2;

    private static final int USB_BAUD_RATE_DEFAULT = 115200;
    private static final int USB_DATA_BITS_DEFAULT = 8;
    private static final int USB_STOP_BITS_DEFAULT = UsbSerialPort.STOPBITS_1;
    private static final int USB_PARITY_DEFAULT = UsbSerialPort.PARITY_NONE;

    private int _baudRate;
    private int _dataBits;
    private int _stopBits;
    private int _parity;
    private boolean _enableDtr;
    private boolean _enableRts;

    private String _usbDeviceName;
    private UsbSerialPort _usbPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb_connect);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        _baudRate = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.PORTS_USB_SERIAL_SPEED, String.valueOf(USB_BAUD_RATE_DEFAULT)));
        _dataBits = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.PORTS_USB_DATA_BITS, String.valueOf(USB_DATA_BITS_DEFAULT)));
        _stopBits = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.PORTS_USB_STOP_BITS, String.valueOf(USB_STOP_BITS_DEFAULT)));
        _parity = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.PORTS_USB_PARITY, String.valueOf(USB_PARITY_DEFAULT)));
        _enableDtr = sharedPreferences.getBoolean(PreferenceKeys.PORTS_USB_DTR, false);
        _enableRts = sharedPreferences.getBoolean(PreferenceKeys.PORTS_USB_RTS, false);

        ProgressBar progressBarUsb = findViewById(R.id.progressBarUsb);
        progressBarUsb.setVisibility(View.VISIBLE);
        ObjectAnimator.ofInt(progressBarUsb, "progress", 10)
                .setDuration(300)
                .start();
        connectUsb();
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

    private UsbSerialProber getCustomProber() {
        ProbeTable customTable = new ProbeTable();
        // Spark Fun
        customTable.addProduct(0x1b4f, 0x9203, CdcAcmSerialDriver.class);
        customTable.addProduct(0x1b4f, 0x9204, CdcAcmSerialDriver.class);
        // Arduino Due
        customTable.addProduct(0x2341, 0x003d, CdcAcmSerialDriver.class);
        // Arduino Uno/Nano (CH34x)
        customTable.addProduct(0x1a86, 0x5523, Ch34xSerialDriver.class);
        customTable.addProduct(0x1a86, 0x7523, Ch34xSerialDriver.class);
        // STM, MCHF
        customTable.addProduct(0x0483, 0x5732, CdcAcmSerialDriver.class);
        // CP2102/2109, iCom
        customTable.addProduct(0x10c4, 0xea60, Cp21xxSerialDriver.class);
        customTable.addProduct(0x10c4, 0xea70, Cp21xxSerialDriver.class);
        customTable.addProduct(0x10c4, 0xea71, Cp21xxSerialDriver.class);
        // FTDI
        customTable.addProduct(0x0403, 0x6001, FtdiSerialDriver.class);
        customTable.addProduct(0x0403, 0x6010, FtdiSerialDriver.class);
        customTable.addProduct(0x0403, 0x6011, FtdiSerialDriver.class);
        customTable.addProduct(0x0403, 0x6014, FtdiSerialDriver.class);
        customTable.addProduct(0x0403, 0x6015, FtdiSerialDriver.class);
        // Raspberry PI Pico
        customTable.addProduct(0x2e8a, 0x0004, CdcAcmSerialDriver.class);
        customTable.addProduct(0x2e8a, 0x0005, CdcAcmSerialDriver.class);
        customTable.addProduct(0x2e8a, 0x000a, CdcAcmSerialDriver.class);
        customTable.addProduct(0x2e8a, 0x000b, CdcAcmSerialDriver.class);
        customTable.addProduct(0x2e8a, 0x000c, CdcAcmSerialDriver.class);
        customTable.addProduct(0x2e8a, 0x000d, CdcAcmSerialDriver.class);
        customTable.addProduct(0x2e8a, 0x000e, CdcAcmSerialDriver.class);
        return new UsbSerialProber(customTable);
    }

    private void connectUsb() {

        new Thread() {
            @Override
            public void run() {
                Message resultMsg = new Message();

                UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
                List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
                if (availableDrivers.isEmpty()) {
                    availableDrivers = getCustomProber().findAllDrivers(manager);
                }
                if (availableDrivers.isEmpty()) {
                    resultMsg.what = USB_NOT_FOUND;
                    onUsbStateChanged.sendMessage(resultMsg);
                    return;
                }

                boolean isFound = false;
                for (int i = 0; i < availableDrivers.size(); i++) {
                    UsbSerialDriver driver = availableDrivers.get(i);
                    UsbDeviceConnection connection;
                    try {
                        connection = manager.openDevice(driver.getDevice());
                    } catch (SecurityException e) {
                        e.printStackTrace();
                        Log.e(TAG, "No rights to open device");
                        continue;
                    }
                    if (connection == null) {
                        Log.e(TAG, "Cannot get connection");
                        continue;
                    }
                    List<UsbSerialPort> ports = driver.getPorts();
                    if (ports.isEmpty()) {
                        Log.e(TAG, "Not enough ports");
                        continue;
                    }
                    UsbSerialPort port = ports.get(0);
                    if (port == null) {
                        Log.e(TAG, "Cannot get port");
                        continue;
                    }

                    try {
                        port.open(connection);
                        port.setParameters(_baudRate, _dataBits, _stopBits, _parity);
                        port.setDTR(_enableDtr);
                        port.setRTS(_enableRts);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Cannot set port parameters");
                        continue;
                    }
                    _usbPort = port;
                    _usbDeviceName = port.getClass().getSimpleName().replace("SerialDriver", "");
                    resultMsg.what = USB_CONNECTED;
                    onUsbStateChanged.sendMessage(resultMsg);
                    isFound = true;
                    break;
                }
                if (!isFound) {
                    resultMsg.what = USB_NOT_FOUND;
                    onUsbStateChanged.sendMessage(resultMsg);
                }
            }
        }.start();
    }

    private final Handler onUsbStateChanged = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            String toastMsg;
            Log.i(TAG, "usb state changed " + msg.what);
            if (msg.what == USB_CONNECTED) {
                UsbPortHandler.setPort(_usbPort);
                UsbPortHandler.setName(_usbDeviceName);

                toastMsg = String.format("USB connected %s", _usbDeviceName);

                Intent resultIntent = new Intent();
                resultIntent.putExtra("name", _usbDeviceName);
                setResult(Activity.RESULT_OK, resultIntent);
            } else {
                toastMsg = getString(R.string.usb_connection_failed);
                setResult(Activity.RESULT_CANCELED);
            }
            Toast.makeText(getBaseContext(), toastMsg, Toast.LENGTH_SHORT).show();
            finish();
        }
    };
}
