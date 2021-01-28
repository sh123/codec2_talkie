package com.radio.codec2talkie.protocol;

import java.nio.ByteBuffer;

public class KissParrot extends Kiss {
    private final int BUFFER_SIZE = 3200 * 60 * 5;

    private final ByteBuffer _buffer;

    public KissParrot() {
        super();
        _buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    }
}
