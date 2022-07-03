package com.radio.codec2talkie.protocol;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

public class KissParrot extends Kiss {

    private final int BUFFER_SIZE = 3200 * 60 * 5;  // 5 min at 3200 bps

    private final int PLAYBACK_DELAY_MS = 1000;

    private final ByteBuffer _buffer;

    private Timer _playbackTimer;

    public KissParrot() {
        super();
        _buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    }

    private void playBuffer() {
        synchronized (_buffer) {
            if (_buffer.position() > 0) {
                _buffer.flip();
                try {
                    byte[] b = new byte[_buffer.remaining()];
                    _buffer.get(b);
                    _transport.write(b);
                } catch (IOException | BufferUnderflowException e) {
                    e.printStackTrace();
                }
                _buffer.clear();
            }
        }
    }

    @Override
    protected void receiveKissData(byte[] data, ProtocolCallback protocolCallback) {
        try {
            if (_playbackTimer != null) {
                _playbackTimer.cancel();
                _playbackTimer.purge();
            }
        } catch (IllegalStateException ignored) {}

        try {
            synchronized(_buffer) {
                _buffer.put(data);
            }
            super.receiveKissData(data, protocolCallback);
        } catch (BufferOverflowException e) {
            e.printStackTrace();
        }
        _playbackTimer = new Timer();
        _playbackTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                playBuffer();
            }
        }, PLAYBACK_DELAY_MS);
    }
}
