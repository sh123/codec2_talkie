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

    private final Context _context;
    private final SharedPreferences _sharedPreferences;

    private final long _fskModem;

    public SoundModem(String name, Context context) {
        _name = name;

        _context = context;
        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_context);

        //_fskModem = Codec2.fskCreate(AUDIO_SAMPLE_SIZE, 300, 1600, 200);
        _fskModem = Codec2.fskCreate(AUDIO_SAMPLE_SIZE, 1200, 1200, 1000);

        _recordAudioBuffer = new short[Codec2.fskDemodSamplesBufSize(_fskModem)];
        _recordBitBuffer = new byte[Codec2.fskDemodBitsBufSize(_fskModem)];
        _playbackAudioBuffer = new short[Codec2.fskModSamplesBufSize(_fskModem)];
        _playbackBitBuffer = new byte[Codec2.fskModBitsBufSize(_fskModem)];

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

    public static byte[] toHdlcByteBitArray(byte[] data, boolean shouldBitStuff) {
        StringBuilder s = new StringBuilder();
        StringBuilder s2 = new StringBuilder();
        ByteBuffer bitBuffer = ByteBuffer.allocate(512*8);

        int cntOnes = 0;
        for (int i = 0; i < 8 * data.length; i++) {
            int b = ((int)data[i / 8]) & 0xff;
            // HDLC transmits least significant bit first
            if ((b & (1 << (i % 8))) > 0) {
                bitBuffer.put((byte)1);
                s.append('1');
                if (shouldBitStuff)
                    cntOnes += 1;
            } else {
                bitBuffer.put((byte)0);
                s.append(0);
                cntOnes = 0;
            }
            if (shouldBitStuff && cntOnes == 5) {
                bitBuffer.put((byte)0);
                s.append('0');
                s2.append(' ');
                cntOnes = 0;
            }
            if (i % 8 == 3) s.append(':');
            if (i % 8 == 7) {
                s.append(' ');
                s2.append(String.format("%02x        ", b));
            }
        }

        Log.i(TAG, s2.toString());
        Log.i(TAG, s.toString());

        bitBuffer.flip();
        byte[] r = new byte[bitBuffer.remaining()];
        bitBuffer.get(r);
        return r;
    }

    public byte[] genPreamble(int count) {
        byte[] preamble = new byte[count];
        for (int i = 0; i < count; i++)
            preamble[i] = (byte)0x7e;
        return toHdlcByteBitArray(preamble, false);
    }

    public byte[] hdlcEncode(byte[] dataSrc) {
        ByteBuffer buffer = ByteBuffer.allocate(512);

        buffer.put(dataSrc);
        int fcs = ChecksumTools.calculateFcs(dataSrc);
        //least significant byte first
        buffer.put((byte)(fcs & 0xff));
        buffer.put((byte)((fcs >> 8) & 0xff));

        buffer.flip();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);

        //Log.i(TAG, String.format("checksum: %x", fcs));
        Log.i(TAG, "" + Arrays.toString(data));

        byte[] dataBytesAsBits = toHdlcByteBitArray(data, true);
        Log.i(TAG, "write() " + data.length + " " + 8 * data.length + " "  + dataBytesAsBits.length + " " + _playbackBitBuffer.length);

        ByteBuffer hdlcBitBuffer = ByteBuffer.allocate(512*8);
        hdlcBitBuffer.put(genPreamble(30));
        hdlcBitBuffer.put(dataBytesAsBits);
        hdlcBitBuffer.put(genPreamble(5));

        hdlcBitBuffer.flip();
        byte[] r = new byte[hdlcBitBuffer.remaining()];
        hdlcBitBuffer.get(r);
        return r;
    }

    public byte[] toNrzi(byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(data.length);
        byte last = 0;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == 0) {
                last = last == 0 ? (byte)1 : (byte)0;
            }
            buffer.put(last);
        }
        buffer.flip();
        byte[] r = new byte[buffer.remaining()];
        buffer.get(r);
        return r;
    }

    @Override
    public int write(byte[] dataSrc) throws IOException {
        byte[] dataBytesAsBits = toNrzi(hdlcEncode(dataSrc));

        int j = 0;
        StringBuilder ss = new StringBuilder();
        for (int i = 0; i < dataBytesAsBits.length; i++, j++) {
            if (j >= _playbackBitBuffer.length) {
                Log.i(TAG, "-- " + i + " " + j);
                Codec2.fskModulate(_fskModem, _playbackAudioBuffer, _playbackBitBuffer);
                StringBuilder s = new StringBuilder();
                for (int x = 0; x < _playbackAudioBuffer.length; x++) {
                    s.append(_playbackAudioBuffer[x]);
                    s.append(' ');
                }
                Log.i(TAG, s.toString());
                int r = _systemAudioPlayer.write(_playbackAudioBuffer, 0, _playbackAudioBuffer.length);
                Log.i(TAG, "---- result " + r);
                _systemAudioPlayer.play();
                j = 0;
            }
            _playbackBitBuffer[j] = dataBytesAsBits[i];
            if (dataBytesAsBits[i] == 1)
                ss.append('1');
            else
                ss.append('0');
        }
        Log.i(TAG, ss.toString());
        Log.i(TAG, "-- " + j);
        Codec2.fskModulate(_fskModem, _playbackAudioBuffer, Arrays.copyOf(_playbackBitBuffer, j));
        int r = _systemAudioPlayer.write(_playbackAudioBuffer, 0, _playbackAudioBuffer.length);
        Log.i(TAG, "---- result " + r);
        StringBuilder s = new StringBuilder();
        for (int x = 0; x < _playbackAudioBuffer.length; x++) {
            s.append(_playbackAudioBuffer[x]);
            s.append(' ');
        }
        Log.i(TAG, s.toString());
        _systemAudioPlayer.play();
        return 0;
    }

    @Override
    public void close() throws IOException {
        Codec2.fskDestroy(_fskModem);
    }
}
