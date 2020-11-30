package com.radio.codec2talkie;

import android.bluetooth.BluetoothSocket;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.ustadmobile.codec2.Codec2;

public class Codec2Player extends Thread {

    private final InputStream _btInputStream;
    private final OutputStream _btOutputStream;

    private final int AudioSampleRate = 8000;

    private final AudioRecord _audioRecorder;
    private final int _audioRecorderMinBufferSize;

    private final AudioTrack _audioPlayer;
    private final int _audioPlayerMinBufferSize;

    private final long _codec2Con;

    public Codec2Player(BluetoothSocket btSocket) throws IOException {

        _btInputStream = btSocket.getInputStream();
        _btOutputStream = btSocket.getOutputStream();

        _audioRecorderMinBufferSize = AudioRecord.getMinBufferSize(
                AudioSampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        _audioRecorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                AudioSampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                _audioRecorderMinBufferSize);

        _audioPlayerMinBufferSize = AudioTrack.getMinBufferSize(
                AudioSampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        _audioPlayer = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(AudioSampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(_audioPlayerMinBufferSize)
                .build();

        _codec2Con = Codec2.create(Codec2.CODEC2_MODE_1300);
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // read from bluetooth, decode and playback
            try {
                int countBytes = _btInputStream.available();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // read from mic, encode and write to bluetooth
        }
    }
}
