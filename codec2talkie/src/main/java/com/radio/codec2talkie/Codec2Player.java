package com.radio.codec2talkie;

import android.bluetooth.BluetoothSocket;
import android.media.AudioAttributes;
import android.media.AudioDeviceCallback;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.radio.codec2talkie.kiss.KissCallback;
import com.radio.codec2talkie.kiss.KissProcessor;
import com.ustadmobile.codec2.Codec2;

public class Codec2Player extends Thread {

    public static int PLAYER_DISCONNECT = 1;
    public static int PLAYER_LISTENING = 2;
    public static int PLAYER_RECORDING = 3;
    public static int PLAYER_PLAYING = 4;
    public static int PLAYER_RX_LEVEL = 5;
    public static int PLAYER_TX_LEVEL = 6;

    private static int AUDIO_MIN_LEVEL = -70;
    private static int AUDIO_HIGH_LEVEL = -15;

    private final int AUDIO_SAMPLE_SIZE = 8000;
    private final int SLEEP_IDLE_DELAY_MS = 20;

    private final int RX_TIMEOUT = 100;
    private final int TX_TIMEOUT = 2000;

    private final byte CSMA_PERSISTENCE = (byte)0xff;
    private final byte CSMA_SLOT_TIME = (byte)0x00;

    private final int RX_BUFFER_SIZE = 8192;

    private long _codec2Con;

    private BluetoothSocket _btSocket;
    private UsbSerialPort _usbPort;

    private int _audioBufferSize;

    private boolean _isRecording = false;
    private int _currentStatus = PLAYER_DISCONNECT;

    // input data, bt -> audio
    private InputStream _btInputStream;

    private final AudioTrack _audioPlayer;

    private short[] _playbackAudioBuffer;

    // output data., mic -> bt
    private OutputStream _btOutputStream;

    private final AudioRecord _audioRecorder;

    private final byte[] _rxDataBuffer;
    private short[] _recordAudioBuffer;
    private char[] _recordAudioEncodedBuffer;

    // loopback mode
    private boolean _isLoopbackMode;
    private ByteBuffer _loopbackBuffer;

    // callbacks
    private KissProcessor _kissProcessor;
    private final Handler _onPlayerStateChanged;

    public Codec2Player(Handler onPlayerStateChanged, int codec2Mode) {
        _onPlayerStateChanged = onPlayerStateChanged;
        _isLoopbackMode = false;
        _rxDataBuffer = new byte[RX_BUFFER_SIZE];

        setCodecModeInternal(codec2Mode);

        int _audioRecorderMinBufferSize = AudioRecord.getMinBufferSize(
                AUDIO_SAMPLE_SIZE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        _audioRecorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                AUDIO_SAMPLE_SIZE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                3 * _audioRecorderMinBufferSize);
        _audioRecorder.startRecording();

        int _audioPlayerMinBufferSize = AudioTrack.getMinBufferSize(
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
    }

    public void setSocket(BluetoothSocket btSocket) throws IOException {
        _btSocket = btSocket;
        _btInputStream = _btSocket.getInputStream();
        _btOutputStream = _btSocket.getOutputStream();
    }

    public void setUsbPort(UsbSerialPort port) {
        _usbPort = port;
    }

    public void setLoopbackMode(boolean isLoopbackMode) {
        _isLoopbackMode = isLoopbackMode;
    }

    public void setCodecMode(int codecMode) {
        Codec2.destroy(_codec2Con);
        setCodecModeInternal(codecMode);
    }

    public static int getAudioMinLevel() {
        return AUDIO_MIN_LEVEL;
    }

    public static int getAudioHighLevel() {
        return AUDIO_HIGH_LEVEL;
    }

    public void startPlayback() {
        _isRecording = false;
    }

    public void startRecording() {
        _isRecording = true;
    }

    private void setCodecModeInternal(int codecMode) {
        _codec2Con = Codec2.create(codecMode);

        _audioBufferSize = Codec2.getSamplesPerFrame(_codec2Con);
        int _audioEncodedBufferSize = Codec2.getBitsSize(_codec2Con); // returns number of bytes

        _recordAudioBuffer = new short[_audioBufferSize];
        _recordAudioEncodedBuffer = new char[_audioEncodedBufferSize];

        _playbackAudioBuffer = new short[_audioBufferSize];

        _loopbackBuffer = ByteBuffer.allocateDirect(1024 * _audioEncodedBufferSize);

        _kissProcessor = new KissProcessor(_audioEncodedBufferSize, CSMA_PERSISTENCE, CSMA_SLOT_TIME, _kissCallback);
    }

    private final KissCallback _kissCallback = new KissCallback() {
        @Override
        protected void sendByte(byte b) throws IOException {
            if (_isLoopbackMode) {
                try {
                    _loopbackBuffer.put(b);
                } catch (BufferOverflowException e) {
                    e.printStackTrace();
                }
            } else {
                byte [] ba = new byte[1];
                ba[0] = b;
                if (_btOutputStream != null)
                    _btOutputStream.write(b);
                if (_usbPort != null) {
                    _usbPort.write(ba, TX_TIMEOUT);
                }
            }
        }

        @Override
        protected void receiveFrame(byte[] frame) {
            Codec2.decode(_codec2Con, _playbackAudioBuffer, frame);
            notifyAudioLevel(_playbackAudioBuffer, false);
            _audioPlayer.write(_playbackAudioBuffer, 0, _audioBufferSize);
        }
    };

    private void notifyAudioLevel(short [] audioSamples, boolean isTx) {
        double db = getAudioMinLevel();
        if (audioSamples != null) {
            double acc = 0;
            for (short v : audioSamples) {
                acc += Math.abs(v);
            }
            double avg = acc / audioSamples.length;
            db = (20.0 * Math.log10(avg / 32768.0));
        }
        Message msg = Message.obtain();
        if (isTx)
            msg.what = PLAYER_TX_LEVEL;
        else
            msg.what = PLAYER_RX_LEVEL;
        msg.arg1 = (int)db;
        _onPlayerStateChanged.sendMessage(msg);
    }

    private void processRecording() throws IOException {
        _audioRecorder.read(_recordAudioBuffer, 0, _audioBufferSize);
        notifyAudioLevel(_recordAudioBuffer, true);
        Codec2.encode(_codec2Con, _recordAudioBuffer, _recordAudioEncodedBuffer);

        byte [] frame = new byte[_recordAudioEncodedBuffer.length];

        for (int i = 0; i < _recordAudioEncodedBuffer.length; i++) {
            frame[i] = (byte)_recordAudioEncodedBuffer[i];
        }
        _kissProcessor.sendFrame(frame);
    }

    private boolean processLoopbackPlayback() {
        try {
            byte b = _loopbackBuffer.get();
            _kissProcessor.receiveByte(b);
            return true;
        } catch (BufferUnderflowException e) {
            return false;
        }
    }

    private boolean processPlayback() throws IOException {
        if (_isLoopbackMode) {
            return processLoopbackPlayback();
        }
        int bytesRead = 0;
        if (_btInputStream != null) {
            bytesRead = _btInputStream.available();
            if (bytesRead > 0) {
                bytesRead = _btInputStream.read(_rxDataBuffer);
            }
        }
        if (_usbPort != null) {
            bytesRead = _usbPort.read(_rxDataBuffer, RX_TIMEOUT);
        }
        if (bytesRead > 0) {
            for (int i = 0; i < bytesRead; i++) {
                _kissProcessor.receiveByte(_rxDataBuffer[i]);
            }
            return true;
        }
        return false;
    }

    private void processRecordPlaybackToggle() throws IOException {
        // playback -> recording
        if (_isRecording && _audioRecorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            _audioRecorder.startRecording();
            _audioPlayer.stop();
            _loopbackBuffer.clear();
            notifyAudioLevel(null, false);
        }
        // recording -> playback
        if (!_isRecording && _audioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            _audioRecorder.stop();
            _audioPlayer.play();
            _kissProcessor.flush();
            _loopbackBuffer.flip();
            notifyAudioLevel(null, true);
        }
    }

    private void cleanup() {
        try {
            _kissProcessor.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        _audioRecorder.stop();
        _audioRecorder.release();

        _audioPlayer.stop();
        _audioPlayer.release();

        Codec2.destroy(_codec2Con);
    }

    private void setStatus(int status) {
        if (status != _currentStatus) {
            _currentStatus = status;
            Message msg = Message.obtain();
            msg.what = status;
            _onPlayerStateChanged.sendMessage(msg);
        }
    }

    @Override
    public void run() {
        setPriority(Thread.MAX_PRIORITY);
        try {
            if (!_isLoopbackMode) {
                _kissProcessor.setupCsma();
            }
            while (true) {
                processRecordPlaybackToggle();

                // recording
                if (_audioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    processRecording();
                    setStatus(PLAYER_RECORDING);
                } else {
                    // playback
                    if (processPlayback()) {
                        setStatus(PLAYER_PLAYING);
                    // idling
                    } else {
                        try {
                            Thread.sleep(SLEEP_IDLE_DELAY_MS);
                            if (_currentStatus != PLAYER_LISTENING) {
                                notifyAudioLevel(null, false);
                                notifyAudioLevel(null, true);
                            }
                            setStatus(PLAYER_LISTENING);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        cleanup();
        setStatus(PLAYER_DISCONNECT);
    }
}
