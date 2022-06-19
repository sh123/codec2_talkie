package com.radio.codec2talkie.transport;

import android.bluetooth.BluetoothGatt;
import java.io.IOException;

public class Ble implements Transport {

    private final BluetoothGatt _gatt;

    public Ble(BluetoothGatt gatt) throws IOException {
        _gatt = gatt;
    }

    @Override
    public int read(byte[] data) throws IOException {
        return 0;
    }

    @Override
    public int write(byte[] data) throws IOException {
        return 0;
    }

    @Override
    public void close() throws IOException {
    }
}
