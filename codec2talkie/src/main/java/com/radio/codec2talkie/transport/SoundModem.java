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

import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.tools.BitTools;
import com.radio.codec2talkie.tools.ChecksumTools;
import com.ustadmobile.codec2.Codec2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class SoundModem implements Transport {

    private static final String TAG = SoundModem.class.getSimpleName();

    private static final int AUDIO_SAMPLE_SIZE = 48000;

    private final String _name;

    private AudioTrack _systemAudioPlayer;
    private AudioRecord _systemAudioRecorder;

    private final short[] _recordAudioBuffer;
    private final byte[] _recordBitBuffer;
    private final short[] _playbackAudioBuffer;
    private final byte[] _playbackBitBuffer;
    private final int _samplesPerSymbol;

    private final Context _context;
    private final SharedPreferences _sharedPreferences;

    private final long _fskModem;

    public SoundModem(Context context) {
        _context = context;
        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_context);

        String type = _sharedPreferences.getString(PreferenceKeys.PORTS_SOUND_MODEM_TYPE, "1200");
        _name = "SoundModem" + type;
        if (type.equals("300")) {
            _fskModem = Codec2.fskCreate(AUDIO_SAMPLE_SIZE, 300, 1600, 200);
        } else {
            _fskModem = Codec2.fskCreate(AUDIO_SAMPLE_SIZE, 1200, 1200, 1000);
        }

        _recordAudioBuffer = new short[Codec2.fskDemodSamplesBufSize(_fskModem)];
        _recordBitBuffer = new byte[Codec2.fskDemodBitsBufSize(_fskModem)];
        _playbackAudioBuffer = new short[Codec2.fskModSamplesBufSize(_fskModem)];
        _playbackBitBuffer = new byte[Codec2.fskModBitsBufSize(_fskModem)];
        _samplesPerSymbol = Codec2.fskSamplesPerSymbol(_fskModem);

        constructSystemAudioDevices();
    }

    private void constructSystemAudioDevices() {
        int audioRecorderMinBufferSize = AudioRecord.getMinBufferSize(
                AUDIO_SAMPLE_SIZE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        int audioSource = MediaRecorder.AudioSource.MIC;
        _systemAudioRecorder = new AudioRecord(
                audioSource,
                AUDIO_SAMPLE_SIZE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                audioRecorderMinBufferSize);

        int audioPlayerMinBufferSize = AudioTrack.getMinBufferSize(
                AUDIO_SAMPLE_SIZE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        int usage = AudioAttributes.USAGE_MEDIA;
        _systemAudioPlayer = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(usage)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(AUDIO_SAMPLE_SIZE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(audioPlayerMinBufferSize)
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
    public int write(byte[] srcDataBytesAsBits) throws IOException {
        byte[] dataBytesAsBits = BitTools.convertToNRZI(srcDataBytesAsBits);

        int j = 0;
        for (int i = 0; i < dataBytesAsBits.length; i++, j++) {
            if (j >= _playbackBitBuffer.length) {
                Codec2.fskModulate(_fskModem, _playbackAudioBuffer, _playbackBitBuffer);
                _systemAudioPlayer.write(_playbackAudioBuffer, 0, _playbackAudioBuffer.length);
                _systemAudioPlayer.play();
                j = 0;
            }
            _playbackBitBuffer[j] = dataBytesAsBits[i];
        }

        // process tail
        byte [] bitBufferTail = Arrays.copyOf(_playbackBitBuffer, j);
        Codec2.fskModulate(_fskModem, _playbackAudioBuffer, bitBufferTail);
        _systemAudioPlayer.write(_playbackAudioBuffer, 0, bitBufferTail.length * _samplesPerSymbol);
        _systemAudioPlayer.play();
        return 0;
    }

    @Override
    public void close() throws IOException {
        Codec2.fskDestroy(_fskModem);
    }
}
