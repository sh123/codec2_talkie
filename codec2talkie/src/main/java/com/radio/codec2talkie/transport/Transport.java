package com.radio.codec2talkie.transport;

import java.io.IOException;

public interface Transport {
    String name();
    int read(byte[] data) throws IOException;
    int write(byte[] data) throws IOException;
    int read(short[] data) throws IOException;
    int write(short[] data) throws IOException;
    void close() throws IOException;
}
