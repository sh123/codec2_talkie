package com.radio.codec2talkie;

import android.bluetooth.BluetoothSocket;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;

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
    public static int PLAYER_DISCONNECT = 1;

    private final long _codec2Con;

    private final BluetoothSocket _btSocket;

    private final Handler _onPlayerStateChanged;

    private final int AudioSampleRate = 8000;
    private final int SleepDelayMs = 50;

    private int _audioBufferSize;
    private int _audioEncodedBufferSize;

    private boolean _isRecording = false;

    // input data, bt -> audio
    private final InputStream _btInputStream;

    private final AudioTrack _audioPlayer;
    private final int _audioPlayerMinBufferSize;

    private final short[] _playbackAudioBuffer;
    private final byte[] _playbackAudioEncodedBuffer;

    private int _playbackAudioAudioEncodedBufferIndex;

    // output data., mic -> bt
    private final OutputStream _btOutputStream;

    private final AudioRecord _audioRecorder;
    private final int _audioRecorderMinBufferSize;

    private final short[] _recordAudioBuffer;
    private final char[] _recordAudioEncodedBuffer;

    public Codec2Player(BluetoothSocket btSocket, Handler onPlayerStateChanged) throws IOException {

        _onPlayerStateChanged = onPlayerStateChanged;

        _btSocket = btSocket;
        _btInputStream = _btSocket.getInputStream();
        _btOutputStream = _btSocket.getOutputStream();

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

        _codec2Con = Codec2.create(Codec2.CODEC2_MODE_700C);

        _audioBufferSize = Codec2.getSamplesPerFrame(_codec2Con);
        _audioEncodedBufferSize = (Codec2.getBitsSize(_codec2Con) + 7) / 8;

        _recordAudioBuffer = new short[_audioBufferSize];
        _recordAudioEncodedBuffer = new char[_audioEncodedBufferSize];

        _playbackAudioAudioEncodedBufferIndex = 0;

        _playbackAudioBuffer = new short[_audioBufferSize];
        _playbackAudioEncodedBuffer = new byte[_audioEncodedBufferSize];
    }

    private void processRecording() throws IOException {
        _audioRecorder.read(_recordAudioBuffer, 0, _audioBufferSize);
        Codec2.encode(_codec2Con, _recordAudioBuffer, _recordAudioEncodedBuffer);

        _btOutputStream.write(KISS_FEND);
        _btOutputStream.write(KISS_CMD_DATA);
        for (char c : _recordAudioEncodedBuffer) {
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
        }
        _btOutputStream.write(KISS_FEND);
    }

    private void kissResetState() {
        _kissCmd = KISS_CMD_NOCMD;
        _kissState = KissState.VOID;
    }

    private boolean processPlayback() throws IOException {
        int btBytes = _btInputStream.available();
        if (btBytes > 0) {
            byte[] br = new byte[1];
            int bytesRead = _btInputStream.read(br);
            if (bytesRead == 0) return false;
            byte b = br[0];
            switch (_kissState) {
                case VOID:
                    if (b == KISS_FEND) {
                        _kissCmd = KISS_CMD_NOCMD;
                        _kissState = KissState.GET_CMD;
                    }
                    break;
                case GET_CMD:
                    if (b == KISS_CMD_DATA) {
                        _playbackAudioAudioEncodedBufferIndex = 0;
                        _kissCmd = b;
                        _kissState = KissState.GET_DATA;
                    } else if (b != KISS_FEND) {
                        kissResetState();
                    }
                    break;
                case GET_DATA:
                    if (b == KISS_FESC) {
                        _kissState = KissState.ESCAPE;
                    } else if (b == KISS_FEND) {
                        if (_kissCmd == KISS_CMD_DATA) {
                            Codec2.decode(_codec2Con, _playbackAudioBuffer, _playbackAudioEncodedBuffer);
                            _audioPlayer.write(_playbackAudioBuffer, 0, _audioBufferSize);
                            _playbackAudioAudioEncodedBufferIndex = 0;
                        }
                        kissResetState();
                    } else {
                        _playbackAudioEncodedBuffer[_playbackAudioAudioEncodedBufferIndex++] = b;
                    }
                    break;
                case ESCAPE:
                    if (b == KISS_TFEND) {
                        _playbackAudioEncodedBuffer[_playbackAudioAudioEncodedBufferIndex++] = KISS_FEND;
                        _kissState = KissState.GET_DATA;
                    }
                    else if (b == KISS_TFESC) {
                        _playbackAudioEncodedBuffer[_playbackAudioAudioEncodedBufferIndex++] = KISS_FESC;
                        _kissState = KissState.GET_DATA;
                    }
                    else {
                        kissResetState();
                    }
                    break;
                default:
                    break;
            }
            return true;
        }
        return false;
    }

    public void startPlayback() {
        _isRecording = false;
    }

    public void startRecording() {
        _isRecording = true;
    }

    private void processRecordPlaybackToggle() {
        if (_isRecording && _audioRecorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            _audioRecorder.startRecording();
        }
        if (!_isRecording && _audioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            _audioRecorder.stop();
        }
    }

    @Override
    public void run() {
        try {
            while (_btSocket.isConnected()) {
                processRecordPlaybackToggle();

                if (_audioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    processRecording();
                } else {
                    if (!processPlayback()) {
                        try {
                            Thread.sleep(SleepDelayMs);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        _audioRecorder.stop();
        _audioPlayer.stop();
        Codec2.destroy(_codec2Con);

        Message msg = Message.obtain();
        msg.what = PLAYER_DISCONNECT;
        _onPlayerStateChanged.sendMessage(msg);
    }
}
