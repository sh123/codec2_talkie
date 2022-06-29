package com.radio.codec2talkie.application;

import android.content.Context;

import com.radio.codec2talkie.protocol.Protocol;

import java.io.IOException;

public interface Application {
    void initialize(Protocol protocol, Context context) throws IOException;
    void sendAudioFrame(String dst, byte[] audioFrame) throws IOException;
    void sendTextMessage(String dst, String message) throws IOException;
    void sendPositionReport() throws IOException;
    boolean receive(ApplicationCallback callback) throws IOException;
    void flush() throws IOException;
    void close();
}
