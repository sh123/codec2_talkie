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

    // kiss constants  and state
    private final byte KISS_FEND = (byte)0xc0;
    private final byte KISS_FESC = (byte)0xdb;
    private final byte KISS_TFEND = (byte)0xdc;
    private final byte KISS_TFESC = (byte)0xdd;

    private final byte KISS_CMD_DATA = (byte)0x00;
    private final byte KISS_CMD_NOCMD = (byte)0x80;

    private enum KissState {
        VOID,
        GET_CMD,
        GET_DATA,
        ESCAPE
    };

    private KissState _kissState = KissState.VOID;
    private byte _kissCmd = KISS_CMD_NOCMD;

    // common audio
    private final int AudioSampleRate = 8000;

    private final long _codec2Con;

    // input data, bt -> audio
    private final InputStream _btInputStream;

    private final AudioTrack _audioPlayer;
    private final int _audioPlayerMinBufferSize;

    // output data., mic -> bt
    private final OutputStream _btOutputStream;

    private final AudioRecord _audioRecorder;
    private final int _audioRecorderMinBufferSize;

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

        _codec2Con = Codec2.create(Codec2.CODEC2_MODE_1200);
    }

    private void processRecording() {
        int audioBufferSize = Codec2.getSamplesPerFrame(_codec2Con);
        int encodedBufferSize = (Codec2.getBitsSize(_codec2Con) + 7) / 8;

        short[] recordAudioBuffer = new short[audioBufferSize];
        char[] recordEncodedBuffer = new char[encodedBufferSize];

        _audioRecorder.read(recordAudioBuffer, 0, audioBufferSize);
        Codec2.encode(_codec2Con, recordAudioBuffer, recordEncodedBuffer);

        try {
            _btOutputStream.write(KISS_FEND);
            _btOutputStream.write(KISS_CMD_DATA);
            for (char c : recordEncodedBuffer) {
                byte b = (byte)c;
                if (b == KISS_FEND) {
                    _btOutputStream.write(KISS_FESC);
                    _btOutputStream.write(KISS_TFEND);
                } else if (b == KISS_FESC){
                    _btOutputStream.write(KISS_FESC);
                    _btOutputStream.write(KISS_TFESC);
                } else {
                    _btOutputStream.write(b);
                }
                _btOutputStream.write(KISS_FEND);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processPlayback() {
        try {
            int btBytes = _btInputStream.available();
            if (btBytes > 0) {
                byte[] br = new byte[1];
                int bytesRead = _btInputStream.read(br);
                if (bytesRead == 1) {
                    byte b = br[0];
                    switch (_kissState) {
                        case VOID:
                            if (b == KISS_FEND) {
                                _kissCmd = KISS_CMD_NOCMD;
                                _kissState = KissState.GET_CMD;
                            }
                            break;
                        case GET_CMD:
                            if (b != KISS_FEND) {
                                if (b == KISS_CMD_DATA) {
                                    // reset buffer
                                    _kissCmd = b;
                                    _kissState = KissState.GET_DATA;
                                } else {
                                    _kissCmd = KISS_CMD_NOCMD;
                                    _kissState = KissState.VOID;
                                }
                            }
                            break;
                        case GET_DATA:
                            if (b == KISS_FESC) {
                                _kissState = KissState.ESCAPE;
                            } else if (b == KISS_FEND) {
                                if (_kissCmd == KISS_CMD_DATA) {
                                    // decode buffer and write to audio out
                                }
                                _kissCmd = KISS_CMD_NOCMD;
                                _kissState = KissState.VOID;
                            } else {
                                // add byte to decode buffer
                            }
                            break;
                        case ESCAPE:
                            if (b == KISS_TFEND) {
                                // write FEND to buffer
                                _kissState = KissState.GET_DATA;
                            }
                            else if (b == KISS_TFESC) {
                                // write FESC to buffer
                                _kissState = KissState.GET_DATA;
                            }
                            else {
                                _kissCmd = KISS_CMD_NOCMD;
                                _kissState = KissState.VOID;
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            if (_audioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                processRecording();
            }
            else {
                processPlayback();
            }
        }
    }
}
