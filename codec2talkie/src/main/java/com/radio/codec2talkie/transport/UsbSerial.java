package com.radio.codec2talkie.transport;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.hoho.android.usbserial.driver.SerialTimeoutException;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.tools.TextTools;

import java.io.IOException;
import java.nio.ByteBuffer;

public class UsbSerial implements Transport {

    private static final int RX_TIMEOUT = 5;
    private static final int TX_TIMEOUT = 2000;

    private final UsbSerialPort _usbPort;
    private final String _name;

    private final boolean _isPrefixEnabled;
    private final byte[] _bytePrefix;

    protected SharedPreferences _sharedPreferences;

    public UsbSerial(UsbSerialPort usbPort, String name, Context context) {
        _usbPort = usbPort;
        _name = name;
        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        _isPrefixEnabled = _sharedPreferences.getBoolean(PreferenceKeys.PORTS_USB_IS_PREFIX_ENABLED, false);
        String prefix = _sharedPreferences.getString(PreferenceKeys.PORTS_USB_PREFIX, "");
        _bytePrefix = TextTools.hexStringToByteArray(prefix);
    }

    @Override
    public String name() {
        return _name;
    }

    @Override
    public int read(byte[] data) throws IOException {
        return _usbPort.read(data, RX_TIMEOUT);
    }

    @Override
    public int write(byte[] data) throws IOException {
        try {
            if (_isPrefixEnabled) {
                byte[] pkt = ByteBuffer.allocate(_bytePrefix.length + data.length)
                        .put(_bytePrefix)
                        .put(data)
                        .array();
                _usbPort.write(pkt, TX_TIMEOUT);
            } else {
                _usbPort.write(data, TX_TIMEOUT);
            }
            return data.length;
        } catch (SerialTimeoutException e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public int read(short[] data) throws IOException {
        return 0;
    }

    @Override
    public int write(short[] data) throws IOException {
        return 0;
    }

    @Override
    public void close() throws IOException {
        _usbPort.close();
    }
}
