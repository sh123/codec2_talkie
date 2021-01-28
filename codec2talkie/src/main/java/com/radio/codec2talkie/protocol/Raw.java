package com.radio.codec2talkie.protocol;

import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;
import java.util.Arrays;

public class Raw implements Protocol {

    private final int RX_BUFFER_SIZE = 8192;

    protected Transport _transport;
    protected final byte[] _rxDataBuffer;

    public Raw() {
        _rxDataBuffer = new byte[RX_BUFFER_SIZE];
    }

    public void initialize(Transport transport) {
        _transport = transport;
    }

    public void send(byte [] frame) throws IOException {
        _transport.write(Arrays.copyOf(frame, frame.length));
    }

    public boolean receive(Callback callback) throws IOException {
        int bytesRead = _transport.read(_rxDataBuffer);
        if (bytesRead > 0) {
            callback.onReceive(Arrays.copyOf(_rxDataBuffer, bytesRead));
            return true;
        }
        return false;
    }

    public void flush() {
    }
}
