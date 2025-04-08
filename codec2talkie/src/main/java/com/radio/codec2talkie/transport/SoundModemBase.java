package com.radio.codec2talkie.transport;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Process;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.rigctl.RigCtl;
import com.radio.codec2talkie.rigctl.RigCtlFactory;
import com.radio.codec2talkie.settings.PreferenceKeys;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ShortBuffer;
import java.util.Timer;
import java.util.TimerTask;

public class SoundModemBase implements Runnable {

    private static final String TAG = SoundModemBase.class.getSimpleName();

    protected String _name;
    protected Context _context;
    protected SharedPreferences _sharedPreferences;

    protected AudioTrack _systemAudioPlayer;
    protected AudioRecord _systemAudioRecorder;

    protected boolean _isRunning = true;

    private final RigCtl _rigCtl;
    private Timer _pttOffTimer;
    private boolean _isPttOn;
    private final int _pttOffDelayMs;

    protected final ShortBuffer _recordAudioSampleBuffer;

    protected final boolean _isLoopback;

    public SoundModemBase(Context context, int sampleRate) {
        _name = "SoundModem";
        _isPttOn = false;
        _context = context;

        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        boolean disableRx = _sharedPreferences.getBoolean(PreferenceKeys.PORTS_SOUND_MODEM_DISABLE_RX, false);
        _pttOffDelayMs = Integer.parseInt(_sharedPreferences.getString(PreferenceKeys.PORTS_SOUND_MODEM_PTT_OFF_DELAY_MS, "1000"));
        _isLoopback = _sharedPreferences.getBoolean(PreferenceKeys.PORTS_SOUND_MODEM_LOOPBACK, false);
        if (_isLoopback) _name += "_";

        int audioSource = Integer.parseInt(_sharedPreferences.getString(PreferenceKeys.PORTS_SOUND_MODEM_AUDIO_SOURCE, "6"));
        int audioDestination = Integer.parseInt(_sharedPreferences.getString(PreferenceKeys.PORTS_SOUND_MODEM_AUDIO_DESTINATION, "1"));
        constructSystemAudioDevices(disableRx, sampleRate, audioSource, audioDestination);

        _rigCtl = RigCtlFactory.create(context);
        try {
            _rigCtl.initialize(TransportFactory.create(TransportFactory.TransportType.USB, context), context, null);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to initialize RigCtl");
        }

        _recordAudioSampleBuffer = ShortBuffer.allocate(1024*100);

        if (!disableRx && !_isLoopback)
            new Thread(this).start();
    }

    private void constructSystemAudioDevices(boolean disableRx, int sampleRate, int audioSource, int audioPlaybackUsage) {
        int audioRecorderMinBufferSize = 10 * AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        _systemAudioRecorder = new AudioRecord(
                audioSource,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                audioRecorderMinBufferSize);

        int audioPlayerMinBufferSize = 10 * AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (!disableRx)
            _systemAudioRecorder.startRecording();

        _systemAudioPlayer = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(audioPlaybackUsage)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(audioPlayerMinBufferSize)
                .build();

        Log.i(TAG, "Play buffer size " + audioPlayerMinBufferSize + ", recorder " + audioRecorderMinBufferSize);
    }

    protected int read(short[] sampleBuffer, int samplesToRead) {
        synchronized (_recordAudioSampleBuffer) {
            if (_recordAudioSampleBuffer.position() >= samplesToRead) {
                _recordAudioSampleBuffer.flip();
                _recordAudioSampleBuffer.get(sampleBuffer, 0, samplesToRead);
                _recordAudioSampleBuffer.compact();
                return samplesToRead;
            }
        }
        return 0;
    }

    @Override
    public void run() {
        Log.i(TAG, "Starting receive thread");
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
        int readSize = 32;
        short [] sampleBuf = new short[readSize];
        while (_isRunning) {
            int readCnt = _systemAudioRecorder.read(sampleBuf, 0, readSize);
            if (readCnt != readSize) {
                Log.w(TAG, readCnt + " != " + readSize);
                continue;
            }
            synchronized (_recordAudioSampleBuffer) {
                for (short sample : sampleBuf) {
                    try {
                        _recordAudioSampleBuffer.put(sample);
                    } catch (BufferOverflowException e) {
                        // user is probably transmitting and cannot consume, just discard
                        e.printStackTrace();
                        _recordAudioSampleBuffer.clear();
                    }
                }
            }
        }
    }

    protected void stop() {
        Log.i(TAG, "stop()");
        _isRunning = false;
        _systemAudioRecorder.stop();
        _systemAudioPlayer.stop();
        _systemAudioRecorder.release();
        _systemAudioPlayer.release();
    }

    protected void pttPurge() {
        if (_pttOffTimer != null) {
            _pttOffTimer.cancel();
            _pttOffTimer.purge();
            _pttOffTimer = null;
        }
    }
    protected void pttOn() {
        pttPurge();
        if (_isPttOn) return;
        try {
            _rigCtl.pttOn();
            _isPttOn = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void pttOff() {
        pttPurge();
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
