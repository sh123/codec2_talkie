package com.radio.codec2talkie.transport;

import android.content.Context;
import android.media.AudioTrack;

import java.io.IOException;
import java.nio.BufferOverflowException;

public class SoundModemRaw extends SoundModemBase implements Transport {

    private static final String TAG = SoundModemRaw.class.getSimpleName();

    private static final int SAMPLE_RATE = 8000;    // TODO, need to get from freedv

    public SoundModemRaw(Context context) {
        super(context, SAMPLE_RATE);
    }

    @Override
    public String name() {
        return _name;
    }

    @Override
    public int read(byte[] data) throws IOException {
        return 0;
    }

    @Override
    public int write(byte[] data) throws IOException {
        return 0;
    }

    @Override
    public int read(short[] audioSamples) throws IOException {
        return read(audioSamples, audioSamples.length);
    }

    @Override
    public int write(short[] audioSamples) throws IOException {
        pttOn();
        if (_systemAudioPlayer.getPlayState() != AudioTrack.PLAYSTATE_PLAYING)
            _systemAudioPlayer.play();
        if (_isLoopback) {
            synchronized (_recordAudioSampleBuffer) {
                for (short sample : audioSamples) {
                    try {
                        _recordAudioSampleBuffer.put(sample);
                    } catch (BufferOverflowException e) {
                        // client is transmitting and cannot consume the buffer, just discard
                        _recordAudioSampleBuffer.clear();
                    }
                }
            }
        } else {
            _systemAudioPlayer.write(audioSamples, 0, audioSamples.length);
        }
        _systemAudioPlayer.stop();
        pttOff();
        return audioSamples.length;
    }

    @Override
    public void close() throws IOException {
        stop();
    }
}
