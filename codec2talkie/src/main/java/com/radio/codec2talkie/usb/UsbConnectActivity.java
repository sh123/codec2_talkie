package com.radio.codec2talkie.usb;

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
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.radio.codec2talkie.R;
import com.radio.codec2talkie.settings.PreferenceKeys;

import java.io.IOException;
import java.util.List;

public class UsbConnectActivity extends AppCompatActivity {

    private final int USB_NOT_FOUND = 1;
    private final int USB_CONNECTED = 2;

    private final int USB_BAUD_RATE_DEFAULT = 115200;
    private final int USB_DATA_BITS = 8;
    private final int USB_STOP_BITS = UsbSerialPort.STOPBITS_1;
    private final int USB_PARITY = UsbSerialPort.PARITY_NONE;

    private SharedPreferences _sharedPreferences;

    private String _usbDeviceName;
    private UsbSerialPort _usbPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb_connect);

        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        ProgressBar progressBarUsb = findViewById(R.id.progressBarUsb);
        progressBarUsb.setVisibility(View.VISIBLE);
        ObjectAnimator.ofInt(progressBarUsb, "progress", 10)
                .setDuration(300)
                .start();
        connectUsb();
    }

    private void connectUsb() {

        new Thread() {
            @Override
            public void run() {
                Message resultMsg = new Message();

                UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
                List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
                if (availableDrivers.isEmpty()) {
                    resultMsg.what = USB_NOT_FOUND;
                    onUsbStateChanged.sendMessage(resultMsg);
                    return;
                }

                UsbSerialDriver driver = availableDrivers.get(0);
                UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
                if (connection == null) {
                    resultMsg.what = USB_NOT_FOUND;
                    onUsbStateChanged.sendMessage(resultMsg);
                    return;
                }
                UsbSerialPort port = driver.getPorts().get(0);
                if (port == null) {
                    resultMsg.what = USB_NOT_FOUND;
                    onUsbStateChanged.sendMessage(resultMsg);
                    return;
                }

                try {
                    int baudRate = _sharedPreferences.getInt(PreferenceKeys.PORTS_USB_SERIAL_SPEED, USB_BAUD_RATE_DEFAULT);
                    port.open(connection);
                    port.setParameters(baudRate, USB_DATA_BITS, USB_STOP_BITS, USB_PARITY);
                    port.setDTR(true);
                    port.setRTS(true);
                } catch (IOException e) {
                    resultMsg.what = USB_NOT_FOUND;
                    onUsbStateChanged.sendMessage(resultMsg);
                    return;
                }
                _usbPort = port;
                _usbDeviceName = port.getClass().getSimpleName().replace("SerialDriver","");
                resultMsg.what = USB_CONNECTED;
                onUsbStateChanged.sendMessage(resultMsg);
            }
        }.start();
    }

    private final Handler onUsbStateChanged = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            String toastMsg;
            if (msg.what == USB_CONNECTED) {
                UsbPortHandler.setPort(_usbPort);

                toastMsg = String.format("USB connected %s", _usbDeviceName);
                Toast.makeText(getBaseContext(), toastMsg, Toast.LENGTH_SHORT).show();

                Intent resultIntent = new Intent();
                resultIntent.putExtra("name", _usbDeviceName);
                setResult(Activity.RESULT_OK, resultIntent);
            } else {
                setResult(Activity.RESULT_CANCELED);
            }
            finish();
        }
    };
}
