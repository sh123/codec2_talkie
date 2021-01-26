package com.radio.codec2talkie.transport;

import java.io.IOException;

public interface Transport {
    int read(byte[] data) throws IOException;
    void write(byte[] data) throws IOException;
    void close() throws IOException;
}
