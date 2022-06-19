package com.radio.codec2talkie.transport;

import com.radio.codec2talkie.connect.BleGattWrapper;
import java.io.IOException;

public class Ble implements Transport {

    private final BleGattWrapper _gatt;

    public Ble(BleGattWrapper gatt) throws IOException {
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
