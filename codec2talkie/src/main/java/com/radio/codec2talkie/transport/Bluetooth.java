package com.radio.codec2talkie.transport;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Bluetooth implements Transport {

    private final BluetoothSocket _btSocket;
    private final OutputStream _btOutputStream;
    private final InputStream _btInputStream;
    private final String _name;

    public Bluetooth(BluetoothSocket btSocket, String name) throws IOException {
        _btSocket = btSocket;
        _btInputStream = btSocket.getInputStream();
        _btOutputStream = btSocket.getOutputStream();
        _name = name;
    }

    @Override
    public String name() {
        return _name;
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
    public int write(byte[] data) throws IOException {
        _btOutputStream.write(data);
        return data.length;
    }

    @Override
    public void close() throws IOException {
        _btSocket.close();
    }
}
