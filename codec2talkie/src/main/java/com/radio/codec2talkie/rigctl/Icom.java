package com.radio.codec2talkie.rigctl;

import android.content.Context;
import android.util.Log;

import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class Icom implements RigCtl {
    private static final String TAG = Icom.class.getSimpleName();

    private static final byte PREAMBLE_CODE = (byte)0xfe;
    private static final byte END_CODE = (byte)0xfd;
    private static final byte CTRL_ADDRESS = (byte)0xe0;

    private Transport _transport;

    @Override
    public void initialize(Transport transport, Context context, RigCtlCallback protocolCallback) throws IOException {
        _transport = transport;
    }

    protected byte getCivAddress() {
        return 0;
    }

    @Override
    public void pttOn() throws IOException {
        Log.i(TAG, String.format("PTT ON 0x%x", getCivAddress()));
        ByteBuffer cmd = ByteBuffer.allocate(8);
        cmd.put(PREAMBLE_CODE)
            .put(PREAMBLE_CODE)
            .put(getCivAddress())
            .put(CTRL_ADDRESS)
            .put((byte)0x1c)
            .put((byte)0x00)
            .put((byte)0x01)
            .put(END_CODE);
        _transport.write(cmd.array());
        Log.i(TAG, "PTT ON done");
    }

    @Override
    public void pttOff() throws IOException {
        Log.i(TAG, String.format("PTT OFF 0x%x", getCivAddress()));
        ByteBuffer cmd = ByteBuffer.allocate(8);
        cmd.put(PREAMBLE_CODE)
                .put(PREAMBLE_CODE)
                .put(getCivAddress())
                .put(CTRL_ADDRESS)
                .put((byte)0x1c)
                .put((byte)0x00)
                .put((byte)0x00)
                .put(END_CODE);
        _transport.write(cmd.array());
        Log.i(TAG, "PTT OFF done");
    }
}
