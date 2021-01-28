package com.radio.codec2talkie.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

public class KissParrot extends Kiss {

    private final int BUFFER_SIZE = 3200 * 60 * 5;

    private final int PLAYBACK_DELAY_MS = 2000;

    private final ByteBuffer _buffer;

    private Timer _playbackTimer;

    public KissParrot() {
        super();
        _buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    }

    private void playBuffer() {
        if (_buffer.position() > 0) {
            _buffer.flip();
            try {
                _transport.write(_buffer.array());
            } catch (IOException e) {
                e.printStackTrace();
            }
            _buffer.clear();
        }
    }

    @Override
    protected void receiveKissData(byte[] data, Callback callback) {
        try {
            if (_playbackTimer != null) {
                _playbackTimer.cancel();
                _playbackTimer.purge();
            }
        } catch (IllegalStateException ignored) {}

        _buffer.put(data);
        super.receiveKissData(data, callback);

        _playbackTimer = new Timer();
        _playbackTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                playBuffer();
            }
        }, PLAYBACK_DELAY_MS);
    }
}
