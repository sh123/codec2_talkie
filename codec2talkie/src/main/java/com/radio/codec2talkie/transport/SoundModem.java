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
import com.radio.codec2talkie.tools.AudioTools;
import com.radio.codec2talkie.tools.BitTools;
import com.radio.codec2talkie.tools.DebugTools;
import com.ustadmobile.codec2.Codec2;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class SoundModem implements Transport, Runnable {

    private static final String TAG = SoundModem.class.getSimpleName();

    // NOTE, codec2 library requires that sample_rate % bit_rate == 0
    //public static final int SAMPLE_RATE = 19200;
    public static final int SAMPLE_RATE = 48000;

    private final String _name;

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

    private final ByteBuffer _sampleBuffer;
    private final boolean _isLoopback = false;

    private final long _fskModem;

    private final RigCtl _rigCtl;

    public SoundModem(Context context) {
        _context = context;
        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_context);

        boolean disableRx = _sharedPreferences.getBoolean(PreferenceKeys.PORTS_SOUND_MODEM_DISABLE_RX, false);
        int bitRate = Integer.parseInt(_sharedPreferences.getString(PreferenceKeys.PORTS_SOUND_MODEM_TYPE, "1200"));
        _name = "SoundModem" + bitRate;
        if (bitRate == 300) {
            // <230 spacing for 300 bps does not work with codec2 fsk for receive
            _fskModem = Codec2.fskCreate(SAMPLE_RATE, 300, 1600, 200);
        } else if (bitRate == 1200) {
            _fskModem = Codec2.fskCreate(SAMPLE_RATE, 1200, 1200, 1000);
        } else {
            _fskModem = Codec2.fskCreate(SAMPLE_RATE, 2400, 2165, 1805);
        }

        _recordAudioBuffer = new short[Codec2.fskDemodSamplesBufSize(_fskModem)];
        _recordBitBuffer = new byte[Codec2.fskDemodBitsBufSize(_fskModem)];
        _playbackAudioBuffer = new short[Codec2.fskModSamplesBufSize(_fskModem)];
        _playbackBitBuffer = new byte[Codec2.fskModBitsBufSize(_fskModem)];
        _samplesPerSymbol = Codec2.fskSamplesPerSymbol(_fskModem);
        _bitBuffer = ByteBuffer.allocate(100 * _recordBitBuffer.length);

        constructSystemAudioDevices(disableRx);

        if (_isLoopback)
            _sampleBuffer = ByteBuffer.allocate(100000);
        else
            _sampleBuffer = ByteBuffer.allocate(0);

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
                .setBufferSizeInBytes(audioPlayerMinBufferSize)
                .build();
        _systemAudioPlayer.setVolume(AudioTrack.getMaxVolume());
        _systemAudioPlayer.play();
    }

    @Override
    public String name() {
        return _name;
    }

    @Override
    public int read(byte[] data) throws IOException {
        synchronized (_bitBuffer) {
            if (_bitBuffer.position() > 0) {
                _bitBuffer.flip();
                int len = _bitBuffer.remaining();
                _bitBuffer.get(data, 0, len);
                //Log.v(TAG, "read user: " + DebugTools.byteBitsToFlatString(data));
                _bitBuffer.compact();
                return len;
            }
        }
        return 0;
    }

    @Override
    public int write(byte[] srcDataBytesAsBits) throws IOException {
        //Log.v(TAG, "write     " + DebugTools.byteBitsToFlatString(srcDataBytesAsBits));
        byte[] dataBytesAsBits = BitTools.convertToNRZI(srcDataBytesAsBits);
        //Log.v(TAG, "write NRZ " + DebugTools.byteBitsToFlatString(dataBytesAsBits));
        //Log.v(TAG, "write NRZ " + DebugTools.byteBitsToString(dataBytesAsBits));
        _rigCtl.pttOn();

        int j = 0;
        for (int i = 0; i < dataBytesAsBits.length; i++, j++) {
            if (j >= _playbackBitBuffer.length) {
                Codec2.fskModulate(_fskModem, _playbackAudioBuffer, _playbackBitBuffer);
                //Log.v(TAG, "write samples: " + DebugTools.shortsToHex(_playbackAudioBuffer));
                if (_isLoopback) {
                    synchronized (_sampleBuffer) {
                        for (short sample : _playbackAudioBuffer) {
                            _sampleBuffer.putShort(sample);
                        }
                    }
                    //Log.v(TAG, "pos: " + _sampleBuffer.position() / 2);
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
            synchronized (_sampleBuffer) {
                for (short sample : _playbackAudioBuffer) {
                    _sampleBuffer.putShort(sample);
                }
            }
        } else {
            _systemAudioPlayer.write(_playbackAudioBuffer, 0, bitBufferTail.length * _samplesPerSymbol);
        }
        _rigCtl.pttOff();
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
        byte prevBit = 0;
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
        while (_isRunning) {
            int nin = Codec2.fskNin(_fskModem);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (_isLoopback) {
                synchronized (_sampleBuffer) {
                    if (_sampleBuffer.position() / 2 >= nin) {
                        //Log.i(TAG, "nin: " + nin + ", pos: " + _sampleBuffer.position() / 2);
                        _sampleBuffer.flip();
                        for (int i = 0; i < nin; i++) {
                            _recordAudioBuffer[i] = _sampleBuffer.getShort();
                        }
                        //Log.i(TAG, String.format("%04x", _recordAudioBuffer[0]));
                        _sampleBuffer.compact();
                        //Log.v(TAG, "read samples: " + DebugTools.shortsToHex(_recordAudioBuffer));
                    } else {
                        continue;
                    }
                }
            } else {
                int readCnt = _systemAudioRecorder.read(_recordAudioBuffer, 0, nin);
                // TODO, read tail
                if (readCnt != nin) {
                    Log.w(TAG, "" + readCnt + " != " + nin);
                    continue;
                }
                //Log.v(TAG, "read samples: " + DebugTools.shortsToHex(_recordAudioBuffer));
            }
            //Log.v(TAG, "read audio power: " + AudioTools.getSampleLevelDb(Arrays.copyOf(_recordAudioBuffer, Codec2.fskNin(_fskModem))));
            //Log.v(TAG, readCnt + " " + _recordAudioBuffer.length + " " + Codec2.fskNin(_fskModem));
            Codec2.fskDemodulate(_fskModem, _recordAudioBuffer, _recordBitBuffer);

            //Log.v(TAG, "read NRZ " + DebugTools.byteBitsToFlatString(_recordBitBuffer));
            //Log.v(TAG, "read     " + DebugTools.byteBitsToFlatString(BitTools.convertFromNRZI(_recordBitBuffer, prevBit)));
            synchronized (_bitBuffer) {
                try {
                    _bitBuffer.put(BitTools.convertFromNRZI(_recordBitBuffer, prevBit));
                    prevBit = _recordBitBuffer[_recordBitBuffer.length - 1];
                } catch (BufferOverflowException e) {
                    e.printStackTrace();
                    _bitBuffer.clear();
                }
            }
        }
    }
}
