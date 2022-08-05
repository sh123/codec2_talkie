package com.radio.codec2talkie.rigctl;

import android.content.Context;

import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;
import java.nio.ByteBuffer;

// http://www.ka7oei.com/ft817_meow.html
public class Ft817 implements RigCtl {

    private Transport _transport;

    @Override
    public void initialize(Transport transport, Context context, RigCtlCallback protocolCallback) throws IOException {
        _transport = transport;
    }

    @Override
    public void pttOn() throws IOException {
        // 0x00, 0x00, 0x00, 0x00, 0x08
        // returns 0x00 (was un-keyed), 0xf0 (already keyed)
        ByteBuffer cmd = ByteBuffer.allocate(5);
        cmd.put((byte)0x00).put((byte)0x00).put((byte)0x00).put((byte)0x00).put((byte)0x08);
        _transport.write(cmd.array());
    }

    @Override
    public void pttOff() throws IOException {
        // 0x00, 0x00, 0x00, 0x00, 0xf0
        // returns 0x00 (was keyed), 0xf0 (already un-keyed)
        ByteBuffer cmd = ByteBuffer.allocate(5);
        cmd.put((byte)0x00).put((byte)0x00).put((byte)0x00).put((byte)0x00).put((byte)0xf0);
        _transport.write(cmd.array());
    }
}
