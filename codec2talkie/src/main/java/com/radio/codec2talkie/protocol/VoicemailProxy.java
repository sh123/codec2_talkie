package com.radio.codec2talkie.protocol;

import android.content.Context;

import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;

public class VoicemailProxy implements Protocol {

    Protocol _protocol;

    public VoicemailProxy(Protocol protocol) {
        _protocol = protocol;
    }

    @Override
    public void initialize(Transport transport, Context context) throws IOException {
        _protocol.initialize(transport, context);
    }

    @Override
    public void send(byte[] frame) throws IOException {
        _protocol.send(frame);
        // write to file
    }

    @Override
    public boolean receive(Callback callback) throws IOException {
        return _protocol.receive(new Callback() {
            @Override
            protected void onReceiveAudioFrames(byte[] audioFrames) {
                callback.onReceiveAudioFrames(audioFrames);
                // write to file
            }

            @Override
            protected void onReceiveSignalLevel(byte[] rawData) {
                callback.onReceiveSignalLevel(rawData);
            }
        });
    }

    @Override
    public void flush() throws IOException {
        _protocol.flush();
    }
}
