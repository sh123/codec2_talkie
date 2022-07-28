package com.radio.codec2talkie.transport;

import java.io.IOException;

public class Audio implements Transport {

    private final String _name;

    public Audio(String name) {
        _name = name;
    }

    @Override
    public String name() {
        return _name;
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
