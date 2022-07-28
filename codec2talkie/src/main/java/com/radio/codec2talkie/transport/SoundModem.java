package com.radio.codec2talkie.transport;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.ustadmobile.codec2.Codec2;

import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;

public class SoundModem implements Transport {

    private static final String TAG = SoundModem.class.getSimpleName();

    private static final int AUDIO_SAMPLE_SIZE = 12000;

    private final String _name;

    private AudioTrack _systemAudioPlayer;
    private AudioRecord _systemAudioRecorder;

    private final short[] _recordAudioBuffer;
    private final byte[] _recordBitBuffer;
    private final short[] _playbackAudioBuffer;
    private final byte[] _playbackBitBuffer;

    private final Context _context;
    private final SharedPreferences _sharedPreferences;

    private final long _fskModem;

    public SoundModem(String name, Context context) {
        _name = name;

        _context = context;
        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_context);

        _fskModem = Codec2.fskCreate(AUDIO_SAMPLE_SIZE, 300, 1600, 200);

        _recordAudioBuffer = new short[Codec2.fskDemodSamplesBufSize(_fskModem)];
        _recordBitBuffer = new byte[Codec2.fskDemodBitsBufSize(_fskModem)];
        _playbackAudioBuffer = new short[Codec2.fskModSamplesBufSize(_fskModem)];
        _playbackBitBuffer = new byte[Codec2.fskModBitsBufSize(_fskModem)];

        constructSystemAudioDevices();
    }

    private void constructSystemAudioDevices() {
        int _audioRecorderMinBufferSize = AudioRecord.getMinBufferSize(
                AUDIO_SAMPLE_SIZE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        int audioSource = MediaRecorder.AudioSource.MIC;
        _systemAudioRecorder = new AudioRecord(
                audioSource,
                AUDIO_SAMPLE_SIZE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                _audioRecorderMinBufferSize);

        int _audioPlayerMinBufferSize = AudioTrack.getMinBufferSize(
                AUDIO_SAMPLE_SIZE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        int usage = AudioAttributes.USAGE_MEDIA;
        _systemAudioPlayer = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(usage)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(AUDIO_SAMPLE_SIZE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(_audioPlayerMinBufferSize)
                .build();
    }

    @Override
    public String name() {
        return _name;
    }

    @Override
    public int read(byte[] data) throws IOException {
        return 0;
    }

    public static byte[] toByteBitArray(BitSet bits) {
        byte[] bytes = new byte[bits.length()];
        for (int i=0; i<bits.length(); i++) {
            bytes[i] = (byte) (bits.get(i) ? 1 : 0);
        }
        return bytes;
    }

    @Override
    public int write(byte[] data) throws IOException {
        _systemAudioPlayer.play();
        byte[] dataBits = toByteBitArray(BitSet.valueOf(data));
        Log.i(TAG, "write() " + data.length + " " + dataBits.length + " " + _playbackBitBuffer.length);
        int j = 0;
        for (int i = 0; i < dataBits.length; i++, j++) {
            if (j >= _playbackBitBuffer.length) {
                Log.i(TAG, "-- " + i + " " + j);
                Codec2.fskModulate(_fskModem, _playbackAudioBuffer, _playbackBitBuffer);
                _systemAudioPlayer.write(_playbackAudioBuffer, 0, _playbackAudioBuffer.length);
                j = 0;
            }
            _playbackBitBuffer[j] = dataBits[i];
        }
        Log.i(TAG, "-- " + j);
        Codec2.fskModulate(_fskModem, _playbackAudioBuffer, _playbackBitBuffer);
        _systemAudioPlayer.write(_playbackAudioBuffer, 0, _playbackAudioBuffer.length);
        return 0;
    }

    @Override
    public void close() throws IOException {
        Codec2.fskDestroy(_fskModem);
    }
}
