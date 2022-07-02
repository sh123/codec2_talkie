package com.radio.codec2talkie.protocol;

import android.content.Context;

import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;

public interface Protocol {
    // init
    void initialize(Transport transport, Context context, Callback callback) throws IOException;
    // audio
    int getPcmAudioBufferSize();
    void sendPcmAudio(String src, String dst, int codec, short[] pcmFrame) throws IOException;
    void sendCompressedAudio(String src, String dst, int codec, byte[] frame) throws IOException;
    // data
    void sendData(String src, String dst, byte[] dataPacket) throws IOException;
    // callback
    boolean receive() throws IOException;
    // gps
    void sendPosition(Position position) throws IOException;
    // control
    void flush() throws IOException;
    void close();
}
