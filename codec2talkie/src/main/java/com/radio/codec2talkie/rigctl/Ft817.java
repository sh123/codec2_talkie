package com.radio.codec2talkie.rigctl;

import android.content.Context;
import android.util.Log;

import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;
import java.nio.ByteBuffer;

// http://www.ka7oei.com/ft817_meow.html
public class Ft817 implements RigCtl {
    private static final String TAG = Ft817.class.getSimpleName();

    private Transport _transport;

    @Override
    public void initialize(Transport transport, Context context, RigCtlCallback protocolCallback) throws IOException {
        _transport = transport;
    }

    @Override
    public void pttOn() throws IOException {
        // 0x00, 0x00, 0x00, 0x00, 0x08
        // returns 0x00 (was un-keyed), 0xf0 (already keyed)
        Log.i(TAG, "PTT ON");
        ByteBuffer cmd = ByteBuffer.allocate(5);
        cmd.put((byte)0x00).put((byte)0x00).put((byte)0x00).put((byte)0x00).put((byte)0x08);
        _transport.write(cmd.array());
        Log.i(TAG, "PTT ON done");

        byte[] response = new byte[1];
        int bytesRead = _transport.read(response);
        Log.i(TAG, "PTT ON response: " + bytesRead + " " + response[0]);
    }

    @Override
    public void pttOff() throws IOException {
        // 0x00, 0x00, 0x00, 0x00, 0x88
        // returns 0x00 (was keyed), 0xf0 (already un-keyed)
        Log.i(TAG, "PTT OFF");
        ByteBuffer cmd = ByteBuffer.allocate(5);
        cmd.put((byte)0x00).put((byte)0x00).put((byte)0x00).put((byte)0x00).put((byte)0x88);
        _transport.write(cmd.array());
        Log.i(TAG, "PTT OFF done");

        byte[] response = new byte[1];
        int bytesRead = _transport.read(response);
        Log.i(TAG, "PTT OFF response: " + bytesRead + " " + response[0]);
    }
}
