package com.radio.codec2talkie.transport;

import android.content.Context;
import android.media.AudioTrack;

import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.tools.BitTools;
import com.ustadmobile.codec2.Codec2;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class SoundModemFsk extends SoundModemBase implements Transport {

    private static final String TAG = SoundModemFsk.class.getSimpleName();

    // NOTE, codec2 fsk library requires that sample_rate % bit_rate == 0
    public static final int SAMPLE_RATE = 19200;
    //public static final int SAMPLE_RATE = 48000;

    private final short[] _recordAudioBuffer;
    private final byte[] _recordBitBuffer;
    private final short[] _playbackAudioBuffer;
    private final byte[] _playbackBitBuffer;
    private final ByteBuffer _bitBuffer;

    private final long _fskModem;

    private byte _prevBit;

    public SoundModemFsk(Context context) {
        super(context, SAMPLE_RATE);
        _prevBit = 0;

        int bitRate = Integer.parseInt(_sharedPreferences.getString(PreferenceKeys.PORTS_SOUND_MODEM_TYPE, "1200"));
        int gain = Integer.parseInt(_sharedPreferences.getString(PreferenceKeys.PORTS_SOUND_MODEM_GAIN, "10000"));

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
        _bitBuffer = ByteBuffer.allocate(100 * _recordBitBuffer.length);
    }


    @Override
    public String name() {
        return _name;
    }

    @Override
    public int read(byte[] data) throws IOException {
        int nin = Codec2.fskNin(_fskModem);
        if (read(_recordAudioBuffer, nin) == 0) return 0;

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
        stop();
        Codec2.fskDestroy(_fskModem);
    }
}
