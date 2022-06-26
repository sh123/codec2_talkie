package com.radio.codec2talkie.protocol;

import android.content.Context;

import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;

public class AX25 implements Protocol {

    final Protocol _childProtocol;

    public AX25(Protocol childProtocol) {
        _childProtocol = childProtocol;
    }

    @Override
    public void initialize(Transport transport, Context context) throws IOException {
        _childProtocol.initialize(transport, context);
    }

    @Override
    public void sendAudio(String src, String dst, byte[] frame) throws IOException {
        // build binary packet then send as audio data
        _childProtocol.sendAudio(src, dst, frame);
    }

    @Override
    public void sendData(String src, String dst, byte[] dataPacket) throws IOException {
        // build binary packet then send as data frame (dataPacket is info data)
        _childProtocol.sendData(src, dst, dataPacket);
    }

    @Override
    public boolean receive(Callback callback) throws IOException {
        return _childProtocol.receive(new Callback() {
            @Override
            protected void onReceiveAudioFrames(String src, String dst, byte[] audioFrames) {
                // parse ax25 packet
                //  if data then call callback.onReceiveData
                //      otherwise callback.onReceiveAudioFrames
                callback.onReceiveAudioFrames(src, dst, audioFrames);
            }

            @Override
            protected void onReceiveData(String src, String dst, byte[] data) {
                callback.onReceiveData(src, dst, data);
            }

            @Override
            protected void onReceiveSignalLevel(byte[] rawData) {
                callback.onReceiveSignalLevel(rawData);
            }

            @Override
            protected void onProtocolRxError() {
                callback.onProtocolRxError();
            }
        });
    }

    @Override
    public void flush() throws IOException {
        _childProtocol.flush();
    }

    @Override
    public void close() {
        _childProtocol.close();
    }
}
