package com.radio.codec2talkie.protocol;

import java.io.IOException;

public interface Protocol {
    void initialize(Callback callback) throws IOException;
    void send(byte [] frame) throws IOException;
    void receive(byte[] data);
    void flush() throws IOException;
}
