package com.radio.codec2talkie.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import com.radio.codec2talkie.protocol.ProtocolCallback;
import com.radio.codec2talkie.protocol.Protocol;
import com.radio.codec2talkie.protocol.ProtocolFactory;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.tools.AudioTools;
import com.radio.codec2talkie.tools.DebugTools;
import com.radio.codec2talkie.transport.Transport;
import com.radio.codec2talkie.transport.TransportFactory;

public class AppWorker extends Thread {

    private static final String TAG = AppWorker.class.getSimpleName();

    public static final int PROCESSOR_DISCONNECTED = 1;
    public static final int PROCESSOR_CONNECTED = 2;
    public static final int PROCESSOR_LISTENING = 3;
    public static final int PROCESSOR_TRANSMITTING = 4;
    public static final int PROCESSOR_RECEIVING = 5;
    public static final int PROCESSOR_PLAYING = 6;
    public static final int PROCESSOR_RX_LEVEL = 7;
    public static final int PROCESSOR_TX_LEVEL = 8;
    public static final int PROCESSOR_RX_ERROR = 9;
    public static final int PROCESSOR_TX_ERROR = 10;
    public static final int PROCESSOR_RX_RADIO_LEVEL = 11;

    public static final int PROCESSOR_PROCESS = 12;
    public static final int PROCESSOR_QUIT = 13;
    public static final int PROCESSOR_SEND_LOCATION = 14;

    private static final int AUDIO_MIN_LEVEL = -70;
    private static final int AUDIO_MAX_LEVEL = 0;
    private final int AUDIO_SAMPLE_SIZE = 8000;

    private final int PROCESS_INTERVAL_MS = 20;
    private final int LISTEN_AFTER_MS = 1500;

    private boolean _needsRecording = false;
    private int _currentStatus = PROCESSOR_DISCONNECTED;

    private final Protocol _protocol;
    private final Transport _transport;

    private final int _codec2Mode;

    // input data, bt -> audio
    private AudioTrack _systemAudioPlayer;

    // output data., mic -> bt
    private AudioRecord _systemAudioRecorder;
    private final short[] _recordAudioBuffer;

    // callbacks
    private final Handler _onPlayerStateChanged;
    private Handler _onMessageReceived;
    private final Timer _processPeriodicTimer;

    // listen timer
    private Timer _listenTimer;

    private final Context _context;
    private final SharedPreferences _sharedPreferences;

    public AppWorker(TransportFactory.TransportType transportType, ProtocolFactory.ProtocolType protocolType, int codec2Mode,
                     Handler onPlayerStateChanged, Context context) throws IOException {
        _onPlayerStateChanged = onPlayerStateChanged;

        _context = context;
        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_context);

        _transport  = TransportFactory.create(transportType);
        _protocol = ProtocolFactory.create(protocolType, codec2Mode, context);

        _processPeriodicTimer = new Timer();

        _codec2Mode = codec2Mode;
        _recordAudioBuffer = new short[_protocol.getPcmAudioBufferSize()];

        constructSystemAudioDevices();
    }

    private void constructSystemAudioDevices() {
        int _audioRecorderMinBufferSize = AudioRecord.getMinBufferSize(
                AUDIO_SAMPLE_SIZE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        boolean isVoiceCommunication = _sharedPreferences.getBoolean(PreferenceKeys.APP_AUDIO_INPUT_VOICE_COMMUNICATION, false);
        int audioSource = MediaRecorder.AudioSource.MIC;
        if (isVoiceCommunication) {
            audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
        }
        _systemAudioRecorder = new AudioRecord(
                audioSource,
                AUDIO_SAMPLE_SIZE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                10 * _audioRecorderMinBufferSize);

        int _audioPlayerMinBufferSize = AudioTrack.getMinBufferSize(
                AUDIO_SAMPLE_SIZE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        boolean isSpeakerOutput = _sharedPreferences.getBoolean(PreferenceKeys.APP_AUDIO_OUTPUT_SPEAKER, true);
        int usage = AudioAttributes.USAGE_VOICE_COMMUNICATION;
        if (isSpeakerOutput) {
            usage = AudioAttributes.USAGE_MEDIA;
        }
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
                .setBufferSizeInBytes(10 * _audioPlayerMinBufferSize)
                .build();
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
        if (_currentStatus == PROCESSOR_DISCONNECTED) return;
        Log.i(TAG, "stopRunning()");
        Message msg = new Message();
        msg.what = PROCESSOR_QUIT;
        _onMessageReceived.sendMessage(msg);
    }

    public void sendPosition(Position position) {
        if (_currentStatus == PROCESSOR_DISCONNECTED) return;
        Message msg = new Message();
        msg.what = PROCESSOR_SEND_LOCATION;
        msg.obj = position;
        _onMessageReceived.sendMessage(msg);
    }

    private void sendStatusUpdate(int newStatus, String note) {
        if (newStatus != PROCESSOR_LISTENING) {
            restartListening();
        }
        if (newStatus != _currentStatus) {
            _currentStatus = newStatus;
            Message msg = Message.obtain();
            msg.what = newStatus;
            if (note != null) {
                msg.obj = note;
            }
            _onPlayerStateChanged.sendMessage(msg);
        }
    }

    private void sendRxRadioLevelUpdate(int rssi, int snr) {
        Message msg = Message.obtain();
        msg.what = PROCESSOR_RX_RADIO_LEVEL;
        msg.arg1 = rssi;
        msg.arg2 = snr;
        _onPlayerStateChanged.sendMessage(msg);
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

    private void recordAndSendAudioFrame() throws IOException {
        _systemAudioRecorder.read(_recordAudioBuffer, 0, _recordAudioBuffer.length);
        _protocol.sendPcmAudio(null, null, _codec2Mode, _recordAudioBuffer);
    }

    private final ProtocolCallback _protocolCallback = new ProtocolCallback() {
        @Override
        protected void onReceivePosition(Position position) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void onReceivePcmAudio(String src, String dst, int codec, short[] pcmFrame) {
            String note = (src == null ? "UNK" : src) + "→" + (dst == null ? "UNK" : dst);
            sendStatusUpdate(PROCESSOR_PLAYING, note);
            sendRxAudioLevelUpdate(pcmFrame);
            _systemAudioPlayer.write(pcmFrame, 0, pcmFrame.length);
        }

        @Override
        protected void onReceiveCompressedAudio(String src, String dst, int codec2Mode, byte[] audioFrame) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void onReceiveData(String src, String dst, byte[] data) {
            // handle incoming messages
            Log.i(TAG, src + ">" + dst + ":" + DebugTools.bytesToDebugString(data));
        }

        @Override
        protected void onReceiveSignalLevel(short rssi, short snr) {
            sendRxRadioLevelUpdate(rssi, snr);
        }

        @Override
        protected void onReceiveLog(String logData) {
            Log.i(TAG, logData);
        }

        @Override
        protected void onTransmitPcmAudio(String src, String dst, int codec, short[] frame) {
            String note = (src == null ? "UNK" : src) + "→" + (dst == null ? "UNK" : dst);
            sendStatusUpdate(PROCESSOR_TRANSMITTING, note);
            sendTxAudioLevelUpdate(frame);
        }

        @Override
        protected void onTransmitCompressedAudio(String src, String dst, int codec, byte[] frame) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void onTransmitData(String src, String dst, byte[] data) {
            String note = (src == null ? "UNK" : src) + "→" + (dst == null ? "UNK" : dst);
            sendStatusUpdate(PROCESSOR_TRANSMITTING, note);
        }

        @Override
        protected void onTransmitLog(String logData) {
            Log.i(TAG, logData);
        }

        @Override
        protected void onProtocolRxError() {
            sendStatusUpdate(PROCESSOR_RX_ERROR, null);
            Log.e(TAG, "Protocol RX error");
        }

        @Override
        protected void onProtocolTxError() {
            sendStatusUpdate(PROCESSOR_TX_ERROR, null);
            Log.e(TAG, "Protocol TX error");
        }
    };

    private void restartListening() {
        cancelListening();
        startListening();
    }

    private void startListening() {
        if (_currentStatus == PROCESSOR_LISTENING) {
            return;
        }
        _listenTimer = new Timer();
        _listenTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                onListening();
            }
        }, LISTEN_AFTER_MS);
    }

    private void cancelListening() {
        try {
            if (_listenTimer != null) {
                _listenTimer.cancel();
                _listenTimer.purge();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void onListening() {
        sendRxAudioLevelUpdate(null);
        sendTxAudioLevelUpdate(null);
        sendRxRadioLevelUpdate(0, 0);
        sendStatusUpdate(PROCESSOR_LISTENING, null);
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
        Log.i(TAG, "cleanup() started");
        _systemAudioRecorder.stop();
        _systemAudioRecorder.release();

        _systemAudioPlayer.stop();
        _systemAudioPlayer.release();

        try {
            _protocol.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        _protocol.close();
        try {
            _transport.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "cleanup() completed");
    }

    private void processRxTx() throws IOException {
        processRecordPlaybackToggle();

        // recording
        if (_systemAudioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            recordAndSendAudioFrame();
        } else {
            // playback
            if (_protocol.receive()) {
                sendStatusUpdate(PROCESSOR_RECEIVING, null);
            }
        }
    }

    private void quitProcessing() {
        Log.i(TAG, "quitProcessing()");
        _processPeriodicTimer.cancel();
        _processPeriodicTimer.purge();
        Looper.myLooper().quitSafely();
    }

    private void onProcessorIncomingMessage(Message msg) {
        switch (msg.what) {
            case PROCESSOR_PROCESS:
                try {
                    processRxTx();
                } catch (IOException e) {
                    e.printStackTrace();
                    quitProcessing();
                }
                break;
            case PROCESSOR_QUIT:
                quitProcessing();
                break;
            case PROCESSOR_SEND_LOCATION:
                try {
                    _protocol.sendPosition((Position)msg.obj);
                } catch (IOException e) {
                    e.printStackTrace();
                    quitProcessing();
                }
                break;
            default:
                break;
        }
    }

    private void startProcessorMessageHandler() {
        _onMessageReceived = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                onProcessorIncomingMessage(msg);
            }
        };
        _processPeriodicTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = PROCESSOR_PROCESS;
                _onMessageReceived.sendMessage(msg);
            }
        }, 0, PROCESS_INTERVAL_MS);
    }

    @Override
    public void run() {
        Log.i(TAG, "Starting message loop");
        setPriority(Thread.MAX_PRIORITY);
        Looper.prepare();

        sendStatusUpdate(PROCESSOR_CONNECTED, null);
        _systemAudioPlayer.play();

        try {
            _protocol.initialize(_transport, _context, _protocolCallback);
            startProcessorMessageHandler();
            Looper.loop();
        } catch (IOException e) {
            e.printStackTrace();
        }

        cleanup();
        sendStatusUpdate(PROCESSOR_DISCONNECTED, null);
        Log.i(TAG, "Exiting message loop");
    }
}
