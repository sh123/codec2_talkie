package com.radio.codec2talkie.transport;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class Loopback implements Transport {

    private final int BUFFER_SIZE = 3200 * 60;    // 1 minute for 3200 bps mode

    private final ByteBuffer _buffer;

    private boolean _isReading;

    public Loopback() {
        _buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        _buffer.flip();
        _isReading = true;
    }

    @Override
    public int read(byte[] data) {
        if (!_isReading) {
            _isReading = true;
            _buffer.flip();
        }
        int countRead = 0;
        try {
            for (int i = 0; i < data.length; i++) {
                byte b = _buffer.get();
                data[i] = b;
                countRead++;
            }
        } catch (BufferUnderflowException ignored) {
        }
        return countRead;
    }

    @Override
    public int write(byte[] data) {
        if (_isReading) {
            _buffer.clear();
            _isReading = false;
        }
        int bytesWritten = 0;
        try {
            _buffer.put(data);
            bytesWritten = data.length;
        } catch (BufferOverflowException e) {
            e.printStackTrace();
        }
        return bytesWritten;
    }

    @Override
    public void close() {
    }
}
