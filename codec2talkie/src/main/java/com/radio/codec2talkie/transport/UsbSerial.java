package com.radio.codec2talkie.transport;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;

public class UsbSerial implements Transport {

    private final int RX_TIMEOUT = 100;
    private final int TX_TIMEOUT = 2000;

    private final UsbSerialPort _usbPort;

    public UsbSerial(UsbSerialPort usbPort) {
        _usbPort = usbPort;
    }

    @Override
    public int read(byte[] data) throws IOException {
        return _usbPort.read(data, RX_TIMEOUT);
    }

    @Override
    public int write(byte[] data) throws IOException {
        _usbPort.write(data, TX_TIMEOUT);
        return data.length;
    }

    @Override
    public void close() throws IOException {
        _usbPort.close();
    }
}
