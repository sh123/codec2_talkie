package com.radio.codec2talkie.protocol;

import android.content.Context;

import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;

public interface Protocol {
    void initialize(Transport transport, Context context) throws IOException;
    void send(byte [] frame) throws IOException;
    boolean receive(Callback callback) throws IOException;
    void flush() throws IOException;
}
