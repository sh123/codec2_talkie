package com.radio.codec2talkie.transport;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Bluetooth implements Transport {

    private final BluetoothSocket _btSocket;
    private final OutputStream _btOutputStream;
    private final InputStream _btInputStream;

    public Bluetooth(BluetoothSocket btSocket) throws IOException {
        _btSocket = btSocket;
        _btInputStream = btSocket.getInputStream();
        _btOutputStream = btSocket.getOutputStream();
    }

    @Override
    public int read(byte[] data) throws IOException {
        int bytesRead = _btInputStream.available();
        if (bytesRead > 0) {
            bytesRead = _btInputStream.read(data);
        }
        return bytesRead;
    }

    @Override
    public void write(byte[] data) throws IOException {
        _btOutputStream.write(data);
    }

    @Override
    public void close() throws IOException {
        _btSocket.close();
    }
}
