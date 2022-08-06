package com.radio.codec2talkie.rigctl;

import android.content.Context;
import android.util.Log;

import com.radio.codec2talkie.protocol.Hdlc;
import com.radio.codec2talkie.tools.UnitTools;
import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

// http://www.ka7oei.com/ft817_meow.html
public class Ft817 implements RigCtl {
    private static final String TAG = Ft817.class.getSimpleName();

    private static final int PTT_OFF_DELAY_MS = 1000;

    private Transport _transport;

    private Timer _pttOffTimer;
    private boolean _isPttOn = false;

    @Override
    public void initialize(Transport transport, Context context, RigCtlCallback protocolCallback) throws IOException {
        _transport = transport;
        _isPttOn = false;
    }

    @Override
    public void pttOn() throws IOException {
        if (_isPttOn) return;

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

        _isPttOn = true;
    }

    @Override
    public void pttOff() {
        if (!_isPttOn) return;
        if (_pttOffTimer != null) {
            _pttOffTimer.cancel();
            _pttOffTimer.purge();
            _pttOffTimer = null;
        }
        _pttOffTimer = new Timer();
        _pttOffTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    sendPttOff();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, PTT_OFF_DELAY_MS);
    }

    public void sendPttOff() throws IOException {
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

        _isPttOn = false;
    }
}
