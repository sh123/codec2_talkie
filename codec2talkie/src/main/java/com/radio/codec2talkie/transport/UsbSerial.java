package com.radio.codec2talkie.transport;

import com.hoho.android.usbserial.driver.SerialTimeoutException;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;

public class UsbSerial implements Transport {

    private static final int RX_TIMEOUT = 100;
    private static final int TX_TIMEOUT = 2000;

    private final UsbSerialPort _usbPort;
    private final String _name;

    public UsbSerial(UsbSerialPort usbPort, String name) {
        _usbPort = usbPort;
        _name = name;
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
            _usbPort.write(data, TX_TIMEOUT);
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
