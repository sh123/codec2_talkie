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
import com.radio.codec2talkie.tools.BitTools;
import com.ustadmobile.codec2.Codec2;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class SoundModemFsk implements Transport, Runnable {

    private static final String TAG = SoundModemFsk.class.getSimpleName();

    // NOTE, codec2 fsk library requires that sample_rate % bit_rate == 0
    public static final int SAMPLE_RATE = 19200;
    //public static final int SAMPLE_RATE = 48000;

    private String _name;

    private AudioTrack _systemAudioPlayer;
    private AudioRecord _systemAudioRecorder;

    private final short[] _recordAudioBuffer;
    private final byte[] _recordBitBuffer;
    private final short[] _playbackAudioBuffer;
    private final byte[] _playbackBitBuffer;
    private final int _samplesPerSymbol;
    private final ByteBuffer _bitBuffer;

    private final Context _context;
    private final SharedPreferences _sharedPreferences;

    private boolean _isRunning = true;

    private final ShortBuffer _recordAudioSampleBuffer;

    private final ByteBuffer _sampleBuffer;
    private boolean _isLoopback;

    private final long _fskModem;

    private final RigCtl _rigCtl;
    private Timer _pttOffTimer;
    private boolean _isPttOn;
    private final int _pttOffDelayMs;

    private byte _prevBit;

    public SoundModemFsk(Context context) {
        _context = context;
        _isPttOn = false;
        _prevBit = 0;
        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_context);

        boolean disableRx = _sharedPreferences.getBoolean(PreferenceKeys.PORTS_SOUND_MODEM_DISABLE_RX, false);
        int bitRate = Integer.parseInt(_sharedPreferences.getString(PreferenceKeys.PORTS_SOUND_MODEM_TYPE, "1200"));
        int gain = Integer.parseInt(_sharedPreferences.getString(PreferenceKeys.PORTS_SOUND_MODEM_GAIN, "10000"));
        _pttOffDelayMs = Integer.parseInt(_sharedPreferences.getString(PreferenceKeys.PORTS_SOUND_MODEM_PTT_OFF_DELAY_MS, "1000"));
        _isLoopback = _sharedPreferences.getBoolean(PreferenceKeys.PORTS_SOUND_MODEM_LOOPBACK, false);

        _name = "FSK" + bitRate;
        if (_isLoopback) _name += "_";

        if (bitRate == 300) {
            // <230 spacing for 300 bps does not work with codec2 fsk for receive
            _fskModem = Codec2.fskCreate(SAMPLE_RATE, 300, 1600, 200, gain);
        } else {
            _fskModem = Codec2.fskCreate(SAMPLE_RATE, 1200, 1200, 1000, gain);
        }

        _recordAudioBuffer = new short[Codec2.fskDemodSamplesBufSize(_fskModem)];
        _recordBitBuffer = new byte[Codec2.fskDemodBitsBufSize(_fskModem)];
        _playbackAudioBuffer = new short[Codec2.fskModSamplesBufSize(_fskModem)];
        _playbackBitBuffer = new byte[Codec2.fskModBitsBufSize(_fskModem)];
        _samplesPerSymbol = Codec2.fskSamplesPerSymbol(_fskModem);
        _bitBuffer = ByteBuffer.allocate(100 * _recordBitBuffer.length);

        constructSystemAudioDevices(disableRx);

        _sampleBuffer = ByteBuffer.allocate(_isLoopback ? 1024 * 100 : 0);
        _recordAudioSampleBuffer = ShortBuffer.allocate(_isLoopback ? 1024*100 : 1024*100);

        _rigCtl = RigCtlFactory.create(context);
        try {
            _rigCtl.initialize(TransportFactory.create(TransportFactory.TransportType.USB, context), context, null);
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        int nin = Codec2.fskNin(_fskModem);

        synchronized (_recordAudioSampleBuffer) {
            // read samples to record audio buffer if there is enough data
            if (_recordAudioSampleBuffer.position() >= nin) {
                _recordAudioSampleBuffer.flip();
                try {
                    _recordAudioSampleBuffer.get(_recordAudioBuffer, 0, nin);
                } catch (BufferUnderflowException e) {
                    e.printStackTrace();
                    _recordAudioSampleBuffer.clear();
                    return 0;
                }
                _recordAudioSampleBuffer.compact();
                //Log.i(TAG, "read " + _recordAudioBuffer.position() + " " +audioSamples.length + " " +  DebugTools.shortsToHex(audioSamples));
            // otherwise return void to the user
            } else {
                return 0;
            }
        }
        //Log.v(TAG, "read audio power: " + AudioTools.getSampleLevelDb(Arrays.copyOf(_recordAudioBuffer, Codec2.fskNin(_fskModem))));
        //Log.v(TAG, readCnt + " " + _recordAudioBuffer.length + " " + Codec2.fskNin(_fskModem));
        Codec2.fskDemodulate(_fskModem, _recordAudioBuffer, _recordBitBuffer);

        //Log.v(TAG, "read NRZ " + DebugTools.byteBitsToFlatString(_recordBitBuffer));
        //Log.v(TAG, "read     " + DebugTools.byteBitsToFlatString(BitTools.convertFromNRZI(_recordBitBuffer, prevBit)));
        try {
            _bitBuffer.put(BitTools.convertFromNRZI(_recordBitBuffer, _prevBit));
            _prevBit = _recordBitBuffer[_recordBitBuffer.length - 1];
        } catch (BufferOverflowException e) {
            e.printStackTrace();
            _bitBuffer.clear();
        }
        if (_bitBuffer.position() > 0) {
            _bitBuffer.flip();
            int len = _bitBuffer.remaining();
            _bitBuffer.get(data, 0, len);
            //Log.v(TAG, "read user: " + DebugTools.byteBitsToFlatString(data));
            _bitBuffer.compact();
            return len;
        }
        return 0;
    }

    @Override
    public int write(byte[] srcDataBytesAsBits) throws IOException {
        pttOn();

        //Log.v(TAG, "write     " + DebugTools.byteBitsToFlatString(srcDataBytesAsBits));
        byte[] dataBytesAsBits = BitTools.convertToNRZI(srcDataBytesAsBits);
        //Log.v(TAG, "write NRZ " + DebugTools.byteBitsToFlatString(dataBytesAsBits));
        //Log.v(TAG, "write NRZ " + DebugTools.byteBitsToString(dataBytesAsBits));

        if (_systemAudioPlayer.getPlayState() != AudioTrack.PLAYSTATE_PLAYING)
            _systemAudioPlayer.play();

        int j = 0;
        for (int i = 0; i < dataBytesAsBits.length; i++, j++) {
            if (j >= _playbackBitBuffer.length) {
                Codec2.fskModulate(_fskModem, _playbackAudioBuffer, _playbackBitBuffer);
                //Log.v(TAG, "write samples: " + DebugTools.shortsToHex(_playbackAudioBuffer));
                if (_isLoopback) {
                    synchronized (_recordAudioSampleBuffer) {
                        for (short sample : _playbackAudioBuffer) {
                            try {
                                _recordAudioSampleBuffer.put(sample);
                            } catch (BufferOverflowException e) {
                                // client is transmitting and cannot consume the buffer, just discard
                                _recordAudioSampleBuffer.clear();
                            }
                        }
                    }
                } else {
                    _systemAudioPlayer.write(_playbackAudioBuffer, 0, _playbackAudioBuffer.length);
                }
                j = 0;
            }
            _playbackBitBuffer[j] = dataBytesAsBits[i];
        }

        // process tail
        byte [] bitBufferTail = Arrays.copyOf(_playbackBitBuffer, j);
        Codec2.fskModulate(_fskModem, _playbackAudioBuffer, bitBufferTail);
        if (_isLoopback) {
            for (short sample : _playbackAudioBuffer) {
                try {
                    _recordAudioSampleBuffer.put(sample);
                } catch (BufferOverflowException e) {
                    // client is transmitting and cannot consume the buffer, just discard
                    _recordAudioSampleBuffer.clear();
                }
            }
        } else {
            _systemAudioPlayer.write(_playbackAudioBuffer, 0, _playbackAudioBuffer.length);
        }
        _systemAudioPlayer.stop();
        pttOff();
        return 0;
    }

    @Override
    public int read(short[] data) throws IOException {
        return 0;
    }

    @Override
    public int write(short[] data) throws IOException {
        return 0;
    }

    @Override
    public void close() throws IOException {
        Log.i(TAG, "close()");
        _isRunning = false;
        _systemAudioRecorder.stop();
        _systemAudioPlayer.stop();
        _systemAudioRecorder.release();
        _systemAudioPlayer.release();
        Codec2.fskDestroy(_fskModem);
    }

    @Override
    public void run() {
        Log.i(TAG, "Starting receive thread");
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
        int readSize = 32;
        short[] sampleBuf = new short[readSize];
        while (_isRunning) {
            int readCnt = _systemAudioRecorder.read(sampleBuf, 0, readSize);
            if (readCnt != readSize) {
                Log.w(TAG, "" + readCnt + " != " + readSize);
                continue;
            }
            synchronized (_recordAudioBuffer) {
                for (short sample : sampleBuf) {
                    try {
                        _recordAudioSampleBuffer.put(sample);
                    } catch (BufferOverflowException e) {
                        // user is probably transmitting and cannot consume, just discard
                        _recordAudioSampleBuffer.clear();
                    }
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
