package com.radio.codec2talkie;

import android.bluetooth.BluetoothSocket;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import com.ustadmobile.codec2.Codec2;

public class Codec2Player extends Thread {

    // kiss constants  and state
    private final byte KISS_FEND = (byte)0xc0;
    private final byte KISS_FESC = (byte)0xdb;
    private final byte KISS_TFEND = (byte)0xdc;
    private final byte KISS_TFESC = (byte)0xdd;

    private final byte KISS_CMD_DATA = (byte)0x00;
    private final byte KISS_CMD_NOCMD = (byte)0x80;

    private final int KISS_FRAME_MAX_SIZE = 16;

    private enum KissState {
        VOID,
        GET_CMD,
        GET_DATA,
        ESCAPE
    };

    private KissState _kissState = KissState.VOID;
    private byte _kissCmd = KISS_CMD_NOCMD;

    private int _kissOutputFramePos;
    private int _kissInputFramePos;

    // common audio
    public static int PLAYER_DISCONNECT = 1;

    private final long _codec2Con;

    private final BluetoothSocket _btSocket;

    private final Handler _onPlayerStateChanged;

    private final int AUDIO_SAMPLE_SIZE = 8000;
    private final int SLEEP_DELAY_MS = 10;

    private int _audioBufferSize;
    private int _audioEncodedBufferSize;

    private boolean _isRecording = false;

    // input data, bt -> audio
    private final InputStream _btInputStream;

    private final AudioTrack _audioPlayer;
    private final int _audioPlayerMinBufferSize;

    private final short[] _playbackAudioBuffer;
    private final byte[] _playbackAudioEncodedBuffer;

    // output data., mic -> bt
    private final OutputStream _btOutputStream;

    private final AudioRecord _audioRecorder;
    private final int _audioRecorderMinBufferSize;

    private final short[] _recordAudioBuffer;
    private final char[] _recordAudioEncodedBuffer;

    private ByteBuffer _loopbackBuffer;

    public Codec2Player(BluetoothSocket btSocket, Handler onPlayerStateChanged) throws IOException {

        _onPlayerStateChanged = onPlayerStateChanged;

        _btSocket = btSocket;
        _btInputStream = _btSocket.getInputStream();
        _btOutputStream = _btSocket.getOutputStream();

        _audioRecorderMinBufferSize = AudioRecord.getMinBufferSize(
                AUDIO_SAMPLE_SIZE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        _audioRecorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                AUDIO_SAMPLE_SIZE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                3 * _audioRecorderMinBufferSize);

        _audioPlayerMinBufferSize = AudioTrack.getMinBufferSize(
                AUDIO_SAMPLE_SIZE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        _audioPlayer = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(AUDIO_SAMPLE_SIZE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(3 * _audioPlayerMinBufferSize)
                .build();
        _audioPlayer.play();

        _codec2Con = Codec2.create(Codec2.CODEC2_MODE_700C);

        _audioBufferSize = Codec2.getSamplesPerFrame(_codec2Con);
        _audioEncodedBufferSize = Codec2.getBitsSize(_codec2Con); // returns number of bytes

        _recordAudioBuffer = new short[_audioBufferSize];
        _recordAudioEncodedBuffer = new char[_audioEncodedBufferSize];
        _kissOutputFramePos = 0;

        _playbackAudioBuffer = new short[_audioBufferSize];
        _playbackAudioEncodedBuffer = new byte[_audioEncodedBufferSize];
        _kissInputFramePos = 0;

        _loopbackBuffer = ByteBuffer.allocateDirect(100000);
    }

    private void kissWriteByte(byte b) throws IOException{
        //_btOutputStream.write(b);
        //Log.d("write stream", String.format("%x", b));
        _loopbackBuffer.put(b);
        _kissOutputFramePos++;
    }

    private void kissResetState() {
        _kissCmd = KISS_CMD_NOCMD;
        _kissState = KissState.VOID;
    }

    private void kissStartFrame() throws IOException {
        kissWriteByte(KISS_FEND);
        kissWriteByte(KISS_CMD_DATA);
    }

    private void kissCompleteFrame() throws IOException {
        if (_kissOutputFramePos > 0) {
            kissWriteByte(KISS_FEND);
            _kissOutputFramePos = 0;
        }
    }

    private void kissProcessInputByte(byte b) {
        switch (_kissState) {
            case VOID:
                if (b == KISS_FEND) {
                    _kissCmd = KISS_CMD_NOCMD;
                    _kissState = KissState.GET_CMD;
                }
                break;
            case GET_CMD:
                if (b == KISS_CMD_DATA) {
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
                        // end of packet
                    }
                    kissResetState();
                } else {
                    _playbackAudioEncodedBuffer[_kissInputFramePos++] = b;
                    //Log.d("play", String.format("%x", b));
                }
                break;
            case ESCAPE:
                if (b == KISS_TFEND) {
                    _playbackAudioEncodedBuffer[_kissInputFramePos++] = KISS_FEND;
                    _kissState = KissState.GET_DATA;
                } else if (b == KISS_TFESC) {
                    _playbackAudioEncodedBuffer[_kissInputFramePos++] = KISS_FESC;
                    _kissState = KissState.GET_DATA;
                } else {
                    kissResetState();
                }
                break;
            default:
                break;
        }
        if (_kissInputFramePos >= _audioEncodedBufferSize) {
            Codec2.decode(_codec2Con, _playbackAudioBuffer, _playbackAudioEncodedBuffer);
            _audioPlayer.write(_playbackAudioBuffer, 0, _audioBufferSize);
            _kissInputFramePos = 0;
        }
    }

    private ByteBuffer kissEscape(char [] inputBuffer) {
        ByteBuffer escapedBuffer = ByteBuffer.allocate(4 * inputBuffer.length);
        for (char c : inputBuffer) {
            switch ((byte)c) {
                case KISS_FEND:
                    escapedBuffer.put(KISS_FESC).put(KISS_TFEND).put((byte)c);
                    break;
                case KISS_FESC:
                    escapedBuffer.put(KISS_FESC).put(KISS_TFESC).put((byte)c);
                   break;
                default:
                    escapedBuffer.put((byte)c);
                    break;
            }
        }
        return escapedBuffer;
    }

    private void processRecording() throws IOException {
        _audioRecorder.read(_recordAudioBuffer, 0, _audioBufferSize);
        Codec2.encode(_codec2Con, _recordAudioBuffer, _recordAudioEncodedBuffer);

        ByteBuffer escapedBuffer = kissEscape(_recordAudioEncodedBuffer);
        int numItems = escapedBuffer.position();
        escapedBuffer.rewind();

        if (_kissOutputFramePos == 0) {
            kissStartFrame();
        }
        // new data does not fit, complete and create new frame
        if (numItems +_kissOutputFramePos >= KISS_FRAME_MAX_SIZE) {
            kissCompleteFrame();
            kissStartFrame();
        }
        // write new data
        while (escapedBuffer.position() < numItems) {
            kissWriteByte(escapedBuffer.get());
        }
    }

    private boolean processLoopbackPlayback() {
        try {
            byte b = _loopbackBuffer.get();
            kissProcessInputByte(b);
            return true;
        } catch (BufferUnderflowException e) {
            return false;
        }
    }

    private boolean processPlayback() throws IOException {
        int btBytes = _btInputStream.available();
        if (btBytes > 0) {
            byte[] br = new byte[1];
            int bytesRead = _btInputStream.read(br);
            if (bytesRead == 0) return false;
            kissProcessInputByte(br[0]);
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

    private void processRecordPlaybackToggle() throws IOException {
        if (_isRecording && _audioRecorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            _audioRecorder.startRecording();
            _audioPlayer.stop();

            _loopbackBuffer.clear();
        }
        if (!_isRecording && _audioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            _audioRecorder.stop();
            _audioPlayer.play();

            kissCompleteFrame();
            _kissInputFramePos = 0;

            _loopbackBuffer.flip();
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
                    if (!processLoopbackPlayback()) {
                        try {
                            Thread.sleep(SLEEP_DELAY_MS);
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

        try {
            kissCompleteFrame();
        } catch (IOException e) {
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
