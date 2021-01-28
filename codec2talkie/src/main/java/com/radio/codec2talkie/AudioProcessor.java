package com.radio.codec2talkie;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.util.Arrays;

import com.radio.codec2talkie.protocol.Callback;
import com.radio.codec2talkie.protocol.Kiss;
import com.radio.codec2talkie.tools.AudioTools;
import com.radio.codec2talkie.transport.Transport;
import com.radio.codec2talkie.transport.TransportFactory;
import com.ustadmobile.codec2.Codec2;

public class AudioProcessor extends Thread {

    private static final String TAG = AudioProcessor.class.getSimpleName();

    public static final int PROCESSOR_DISCONNECTED = 1;
    public static final int PROCESSOR_CONNECTED = 2;
    public static final int PROCESSOR_LISTENING = 3;
    public static final int PROCESSOR_RECORDING = 4;
    public static final int PROCESSOR_PLAYING = 5;
    public static final int PROCESSOR_RX_LEVEL = 6;
    public static final int PROCESSOR_TX_LEVEL = 7;

    private static int AUDIO_MIN_LEVEL = -60;
    private static int AUDIO_MAX_LEVEL = 0;

    private final int AUDIO_SAMPLE_SIZE = 8000;
    private final int SLEEP_IDLE_DELAY_MS = 20;
    private final int POST_PLAY_DELAY_MS = 1000;

    private final int RX_BUFFER_SIZE = 8192;

    private long _codec2Con;

    private int _audioBufferSize;
    private int _audioEncodedBufferSize;

    private boolean _isRunning = true;
    private boolean _needsRecording = false;
    private int _currentStatus = PROCESSOR_DISCONNECTED;

    private final Kiss _protocol;
    private final Transport _transport;

    // input data, bt -> audio
    private AudioTrack _systemAudioPlayer;
    private short[] _playbackAudioBuffer;

    // output data., mic -> bt
    private AudioRecord _systemAudioRecorder;
    private final byte[] _rxDataBuffer;
    private short[] _recordAudioBuffer;
    private char[] _recordAudioEncodedBuffer;

    // callbacks
    private final Handler _onPlayerStateChanged;

    public AudioProcessor(TransportFactory.TransportType transportType, Handler onPlayerStateChanged, int codec2Mode) throws IOException {
        _onPlayerStateChanged = onPlayerStateChanged;
        _rxDataBuffer = new byte[RX_BUFFER_SIZE];

        _transport  = TransportFactory.create(transportType);
        _protocol = new Kiss();

        constructCodec2(codec2Mode);
        constructSystemAudioDevices();
    }

    public static int getAudioMinLevel() {
        return AUDIO_MIN_LEVEL;
    }

    public static int getAudioMaxLevel() {
        return AUDIO_MAX_LEVEL;
    }

    public void startPlayback() {
        _needsRecording = false;
    }

    public void startRecording() {
        _needsRecording = true;
    }

    public void stopRunning() {
        _isRunning = false;
    }

    private void constructSystemAudioDevices() {
        int _audioRecorderMinBufferSize = AudioRecord.getMinBufferSize(
                AUDIO_SAMPLE_SIZE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        _systemAudioRecorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                AUDIO_SAMPLE_SIZE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                10 * _audioRecorderMinBufferSize);
        _systemAudioRecorder.startRecording();

        int _audioPlayerMinBufferSize = AudioTrack.getMinBufferSize(
                AUDIO_SAMPLE_SIZE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        _systemAudioPlayer = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(AUDIO_SAMPLE_SIZE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(10 * _audioPlayerMinBufferSize)
                .build();
        _systemAudioPlayer.play();
    }

    private void constructCodec2(int codecMode) {
        _codec2Con = Codec2.create(codecMode);

        _audioBufferSize = Codec2.getSamplesPerFrame(_codec2Con);
        _audioEncodedBufferSize = Codec2.getBitsSize(_codec2Con); // returns number of bytes

        _recordAudioBuffer = new short[_audioBufferSize];
        _recordAudioEncodedBuffer = new char[_audioEncodedBufferSize];

        _playbackAudioBuffer = new short[_audioBufferSize];
    }

    private final Callback _protocolCallback = new Callback() {
        @Override
        protected void onSend(byte[] data) throws IOException {
            _transport.write(data);
        }

        @Override
        protected void onReceive(byte[] data) {
            // split by audio frame and play
            byte [] audioFrame = new byte[_audioEncodedBufferSize];
            for (int i = 0; i < data.length; i += _audioEncodedBufferSize) {
                for (int j = 0; j < _audioEncodedBufferSize && (j + i) < data.length; j++)
                    audioFrame[j] = data[i + j];
                decodeAndPlayAudioFrame(audioFrame);
            }
        }
    };

    private void sendStatusUpdate(int status, int delayMs) {
        if (status == _currentStatus) return;

        _currentStatus = status;
        Message msg = Message.obtain();
        msg.what = status;

        _onPlayerStateChanged.sendMessageDelayed(msg, delayMs);
    }

    private void sendRxAudioLevelUpdate(short [] pcmAudioSamples) {
        Message msg = Message.obtain();
        msg.what = PROCESSOR_RX_LEVEL;
        msg.arg1 = AudioTools.getSampleLevelDb(pcmAudioSamples);
        _onPlayerStateChanged.sendMessage(msg);
    }

    private void sendTxAudioLevelUpdate(short [] pcmAudioSamples) {
        Message msg = Message.obtain();
        msg.what = PROCESSOR_TX_LEVEL;
        msg.arg1 = AudioTools.getSampleLevelDb(pcmAudioSamples);
        _onPlayerStateChanged.sendMessage(msg);
    }

    private void decodeAndPlayAudioFrame(byte[] data) {
        Codec2.decode(_codec2Con, _playbackAudioBuffer, data);

        _systemAudioPlayer.write(_playbackAudioBuffer, 0, _audioBufferSize);
        sendRxAudioLevelUpdate(_playbackAudioBuffer);
    }

    private void recordAndSendAudioFrame() throws IOException {
        sendStatusUpdate(PROCESSOR_RECORDING, 0);

        _systemAudioRecorder.read(_recordAudioBuffer, 0, _audioBufferSize);
        sendTxAudioLevelUpdate(_recordAudioBuffer);

        Codec2.encode(_codec2Con, _recordAudioBuffer, _recordAudioEncodedBuffer);

        byte [] frame = new byte[_recordAudioEncodedBuffer.length];

        for (int i = 0; i < _recordAudioEncodedBuffer.length; i++) {
            frame[i] = (byte)_recordAudioEncodedBuffer[i];
        }
        _protocol.send(frame);
    }

    private boolean receiveAndPlayAudioFrame() throws IOException {
        int bytesRead = _transport.read(_rxDataBuffer);
        if (bytesRead > 0) {
            sendStatusUpdate(PROCESSOR_PLAYING, 0);
            _protocol.receive(Arrays.copyOf(_rxDataBuffer, bytesRead));
            return true;
        }
        return false;
    }

    private void processRecordPlaybackToggle() throws IOException {
        // playback -> recording
        if (_needsRecording && _systemAudioRecorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            _systemAudioPlayer.stop();
            _systemAudioRecorder.startRecording();
            sendRxAudioLevelUpdate(null);
        }
        // recording -> playback
        if (!_needsRecording && _systemAudioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            _protocol.flush();
            _systemAudioRecorder.stop();
            _systemAudioPlayer.play();
            sendTxAudioLevelUpdate(null);
        }
    }

    private void cleanup() {
        _systemAudioRecorder.stop();
        _systemAudioRecorder.release();

        _systemAudioPlayer.stop();
        _systemAudioPlayer.release();

        Codec2.destroy(_codec2Con);

        try {
            _protocol.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            _transport.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean process() throws IOException {
        processRecordPlaybackToggle();

        // recording
        if (_systemAudioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            recordAndSendAudioFrame();
        } else {
            // playback
            if (!receiveAndPlayAudioFrame()) {
                // idling
                if (_currentStatus != PROCESSOR_LISTENING) {
                    sendRxAudioLevelUpdate(null);
                    sendTxAudioLevelUpdate(null);
                }
                sendStatusUpdate(PROCESSOR_LISTENING, POST_PLAY_DELAY_MS);
                return false;
            }
        }
        return true;
    }

    @Override
    public void run() {
        setPriority(Thread.MAX_PRIORITY);

        sendStatusUpdate(PROCESSOR_CONNECTED, 0);

        try {
            sendStatusUpdate(PROCESSOR_LISTENING, 0);
            _protocol.initialize(_protocolCallback);

            while (_isRunning)
                if (!process())
                    Thread.sleep(SLEEP_IDLE_DELAY_MS);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        sendStatusUpdate(PROCESSOR_DISCONNECTED, 0);
        cleanup();
    }
}
