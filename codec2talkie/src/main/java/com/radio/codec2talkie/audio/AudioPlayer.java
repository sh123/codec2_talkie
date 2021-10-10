package com.radio.codec2talkie.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.radio.codec2talkie.MainActivity;
import com.ustadmobile.codec2.Codec2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class AudioPlayer extends Thread {
    private static final String TAG = MainActivity.class.getSimpleName();

    public static final int PLAYER_STARTED = 1;
    public static final int PLAYER_PLAYING_FILE = 2;
    public static final int PLAYER_PLAYED_FILE= 3;
    public static final int PLAYER_ERROR = 4;
    public static final int PLAYER_STOPPED = 4;

    private final Handler _onPlayerStateChanged;
    private final Context _context;
    private final File[] _files;

    private final int AUDIO_SAMPLE_SIZE = 8000;

    private AudioTrack _systemAudioPlayer;
    private short[] _playbackAudioBuffer;

    private long _codec2Con;

    private int _audioBufferSize;
    private int _codec2FrameSize;

    private int _currentStatus = PLAYER_STOPPED;

    public AudioPlayer(File[] files, Handler onPlayerStateChanged, Context context) {

        _onPlayerStateChanged = onPlayerStateChanged;
        _context = context;
        _files = files;

        constructSystemAudioDevices();
    }

    private void constructSystemAudioDevices() {
        int _audioPlayerMinBufferSize = AudioTrack.getMinBufferSize(
                AUDIO_SAMPLE_SIZE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        _systemAudioPlayer = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(AUDIO_SAMPLE_SIZE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(10 * _audioPlayerMinBufferSize)
                .build();
    }

    private void constructCodec2(int codecMode) {
        _codec2Con = Codec2.create(codecMode);

        _audioBufferSize = Codec2.getSamplesPerFrame(_codec2Con);
        _codec2FrameSize = Codec2.getBitsSize(_codec2Con); // returns number of bytes

        _playbackAudioBuffer = new short[_audioBufferSize];
    }

    private void sendStatusUpdate(int newStatus) {
        if (newStatus != _currentStatus) {
            _currentStatus = newStatus;
            Message msg = Message.obtain();
            msg.what = newStatus;

            _onPlayerStateChanged.sendMessage(msg);
        }
    }

    private void playFile(File file) {
        String codec2ModeStr = file.getName().substring(0, 2);
        int codec2Mode = Integer.parseInt(codec2ModeStr);
        constructCodec2(codec2Mode);

        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            sendStatusUpdate(PLAYER_ERROR);
            e.printStackTrace();
            return;
        }

        byte[] codec2Buffer = new byte[_codec2FrameSize];

        try {
            while (inputStream.read(codec2Buffer) == _codec2FrameSize) {
                Codec2.decode(_codec2Con, _playbackAudioBuffer, codec2Buffer);
                _systemAudioPlayer.write(_playbackAudioBuffer, 0, _audioBufferSize);
            }
        } catch (IOException e) {
            sendStatusUpdate(PLAYER_ERROR);
            e.printStackTrace();
            return;
        }

        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void play() {
        _systemAudioPlayer.play();
        for (File file : _files) {
            sendStatusUpdate(PLAYER_PLAYING_FILE);
            playFile(file);
            sendStatusUpdate(PLAYER_PLAYED_FILE);
        }
        _systemAudioPlayer.stop();
        _systemAudioPlayer.release();
    }

    @Override
    public void run() {
        sendStatusUpdate(PLAYER_STARTED);
        play();
        sendStatusUpdate(PLAYER_STOPPED);
    }
}
