package com.radio.codec2talkie.protocol;

import android.content.Context;

import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;

public interface Protocol {
    void initialize(Transport transport, Context context) throws IOException;
    void sendAudio(String src, String dst, byte[] frame) throws IOException;
    void sendData(String src, String dst, byte[] dataPacket) throws IOException;
    boolean receive(Callback callback) throws IOException;
    void flush() throws IOException;
    void close();
}
