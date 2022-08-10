package com.radio.codec2talkie.transport;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Process;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.rigctl.RigCtl;
import com.radio.codec2talkie.rigctl.RigCtlFactory;
import com.radio.codec2talkie.settings.PreferenceKeys;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Timer;
import java.util.TimerTask;

public class SoundModem implements Transport, Runnable {

    private static final String TAG = SoundModem.class.getSimpleName();

    private static final int RECORD_DELAY_MS = 10;
    private static final int SAMPLE_RATE = 8000;

    private final String _name;

    private AudioTrack _systemAudioPlayer;
    private AudioRecord _systemAudioRecorder;

    private boolean _isRunning = true;

    private final RigCtl _rigCtl;
    private Timer _pttOffTimer;
    private boolean _isPttOn;
    private final int _pttOffDelayMs;

    private final ShortBuffer _recordAudioBuffer;

    public SoundModem(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        boolean disableRx = sharedPreferences.getBoolean(PreferenceKeys.PORTS_SOUND_MODEM_DISABLE_RX, false);
        _pttOffDelayMs = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.PORTS_SOUND_MODEM_PTT_OFF_DELAY_MS, "1000"));
        _name = "SndModem";

        constructSystemAudioDevices(disableRx);

        _rigCtl = RigCtlFactory.create(context);
        try {
            _rigCtl.initialize(TransportFactory.create(TransportFactory.TransportType.USB, context), context, null);
        } catch (IOException e) {
            e.printStackTrace();
        }

        _recordAudioBuffer = ShortBuffer.allocate(4096);

        _isPttOn = false;

        if (!disableRx)
            new Thread(this).start();
    }

    private void constructSystemAudioDevices(boolean disableRx) {
        int audioRecorderMinBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        int audioSource = MediaRecorder.AudioSource.VOICE_RECOGNITION;
        _systemAudioRecorder = new AudioRecord(
                audioSource,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                10*audioRecorderMinBufferSize);

        int audioPlayerMinBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (!disableRx)
            _systemAudioRecorder.startRecording();

        int usage = AudioAttributes.USAGE_MEDIA;
        _systemAudioPlayer = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(usage)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(10*audioPlayerMinBufferSize)
                .build();
        _systemAudioPlayer.setVolume(AudioTrack.getMaxVolume());
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
        synchronized (_recordAudioBuffer) {
            if (_recordAudioBuffer.position() >= audioSamples.length) {
                _recordAudioBuffer.flip();
                _recordAudioBuffer.get(audioSamples);
                _recordAudioBuffer.compact();
                return audioSamples.length;
            }
        }
        return 0;
    }

    @Override
    public int write(short[] audioSamples) throws IOException {
        pttOn();
        if (_systemAudioPlayer.getPlayState() != AudioTrack.PLAYSTATE_PLAYING)
            _systemAudioPlayer.play();
        _systemAudioPlayer.write(audioSamples, 0, audioSamples.length);
        _systemAudioPlayer.stop();
        pttOff();
        return audioSamples.length;
    }

    @Override
    public void close() throws IOException {
        Log.i(TAG, "close()");
        _isRunning = false;
        _systemAudioRecorder.stop();
        _systemAudioPlayer.stop();
        _systemAudioRecorder.release();
        _systemAudioPlayer.release();
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
        int readSize = 16;
        short [] sampleBuf = new short[readSize];
        while (_isRunning) {
            try {
                Thread.sleep(RECORD_DELAY_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int readCnt = _systemAudioRecorder.read(sampleBuf, 0, readSize);
            if (readCnt != readSize) {
                Log.w(TAG, "" + readCnt + " != " + readSize);
                continue;
            }
            synchronized (_recordAudioBuffer) {
                for (short sample : sampleBuf) {
                    _recordAudioBuffer.put(sample);
                }
            }
        }
    }

    private void pttOn() {
        if (_isPttOn) return;

        try {
            _rigCtl.pttOn();
            _isPttOn = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pttOff() {
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
                    _rigCtl.pttOff();
                    _isPttOn = false;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, _pttOffDelayMs);
    }
}
