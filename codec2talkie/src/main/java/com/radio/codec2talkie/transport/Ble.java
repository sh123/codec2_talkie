package com.radio.codec2talkie.transport;

import com.radio.codec2talkie.connect.BleGattWrapper;
import java.io.IOException;

public class Ble implements Transport {

    private final BleGattWrapper _gattWrapper;
    private final String _name;

    public Ble(BleGattWrapper gattWrapper, String name) {
        _gattWrapper = gattWrapper;
        _name = name;
    }

    @Override
    public String name() {
        return _name;
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
