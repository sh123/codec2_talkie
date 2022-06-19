package com.radio.codec2talkie.transport;

import com.radio.codec2talkie.connect.BleGattWrapper;
import java.io.IOException;

public class Ble implements Transport {

    private final BleGattWrapper _gattWrapper;

    public Ble(BleGattWrapper gattWrapper) {
        _gattWrapper = gattWrapper;
    }

    @Override
    public int read(byte[] data) throws IOException {
        return _gattWrapper.read(data);
    }

    @Override
    public int write(byte[] data) throws IOException {
        return _gattWrapper.write(data);
    }

    @Override
    public void close() throws IOException {
        _gattWrapper.close();
    }
}
