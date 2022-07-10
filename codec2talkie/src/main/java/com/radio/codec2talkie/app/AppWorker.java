package com.radio.codec2talkie.app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
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

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.storage.log.LogItem;
import com.radio.codec2talkie.storage.log.LogItemRepository;
import com.radio.codec2talkie.protocol.ProtocolCallback;
import com.radio.codec2talkie.protocol.Protocol;
import com.radio.codec2talkie.protocol.ProtocolFactory;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.tools.AudioTools;
import com.radio.codec2talkie.transport.Transport;
import com.radio.codec2talkie.transport.TransportFactory;

public class AppWorker extends Thread {

    private static final String TAG = AppWorker.class.getSimpleName();

    private static final int AUDIO_MIN_LEVEL = -70;
    private static final int AUDIO_MAX_LEVEL = 0;
    private static final int AUDIO_SAMPLE_SIZE = 8000;

    private static final int PROCESS_INTERVAL_MS = 20;
    private static final int LISTEN_AFTER_MS = 1500;

    private boolean _needTransmission = false;
    private AppMessage _currentStatus = AppMessage.EV_DISCONNECTED;

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

    // log integration
    private final LogItemRepository _logItemRepository;

    private final Context _context;
    private final SharedPreferences _sharedPreferences;

    public AppWorker(TransportFactory.TransportType transportType,
                     Handler onPlayerStateChanged, Context context) throws IOException {
        _onPlayerStateChanged = onPlayerStateChanged;

        _context = context;
        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_context);

        String codec2ModeName = _sharedPreferences.getString(PreferenceKeys.CODEC2_MODE, _context.getResources().getStringArray(R.array.codec2_modes)[0]);
        _codec2Mode = AudioTools.extractCodec2ModeId(codec2ModeName);

        _transport  = TransportFactory.create(transportType);
        _protocol = ProtocolFactory.create(_codec2Mode, context);

        _processPeriodicTimer = new Timer();
        _recordAudioBuffer = new short[_protocol.getPcmAudioBufferSize()];

        _logItemRepository = new LogItemRepository((Application)context);

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

    public String getTransportName() {
        return _transport.name();
    }

    public void startReceive() {
        _needTransmission = false;
    }

    public void startTransmit() {
        _needTransmission = true;
    }

    public void stopRunning() {
        if (_currentStatus == AppMessage.EV_DISCONNECTED) return;
        Log.i(TAG, "stopRunning()");
        Message msg = new Message();
        msg.what = AppMessage.CMD_QUIT.toInt();
        _onMessageReceived.sendMessage(msg);
    }

    public void sendPositionToTnc(Position position) {
        if (_currentStatus == AppMessage.EV_DISCONNECTED) return;
        Message msg = new Message();
        msg.what = AppMessage.CMD_SEND_LOCATION_TO_TNC.toInt();
        msg.obj = position;
        _onMessageReceived.sendMessage(msg);
    }

    private void sendStatusUpdate(AppMessage newStatus, String note) {

        if (newStatus != _currentStatus) {
            _currentStatus = newStatus;
            Message msg = Message.obtain();
            msg.what = newStatus.toInt();
            if (note != null) {
                msg.obj = note;
            }
            _onPlayerStateChanged.sendMessage(msg);
        }
        if (newStatus != AppMessage.EV_LISTENING) {
            restartListening();
        }
    }

    private void sendRxRadioLevelUpdate(int rssi, int snr) {
        Message msg = Message.obtain();
        msg.what = AppMessage.EV_RX_RADIO_LEVEL.toInt();
        msg.arg1 = rssi;
        msg.arg2 = snr;
        _onPlayerStateChanged.sendMessage(msg);
    }

    private void sendRxAudioLevelUpdate(short [] pcmAudioSamples) {
        Message msg = Message.obtain();
        msg.what = AppMessage.EV_RX_LEVEL.toInt();
        msg.arg1 = AudioTools.getSampleLevelDb(pcmAudioSamples);
        _onPlayerStateChanged.sendMessage(msg);
    }

    private void sendTxAudioLevelUpdate(short [] pcmAudioSamples) {
        Message msg = Message.obtain();
        msg.what = AppMessage.EV_TX_LEVEL.toInt();
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
            sendStatusUpdate(AppMessage.EV_VOICE_RECEIVED, note);
            sendRxAudioLevelUpdate(pcmFrame);
            _systemAudioPlayer.write(pcmFrame, 0, pcmFrame.length);
        }

        @Override
        protected void onReceiveCompressedAudio(String src, String dst, int codec2Mode, byte[] audioFrame) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void onReceiveData(String src, String dst, byte[] data) {
            // TODO, handle incoming messages
            String note = (src == null ? "UNK" : src) + "→" + (dst == null ? "UNK" : dst);
            sendStatusUpdate(AppMessage.EV_DATA_RECEIVED, note);
        }

        @Override
        protected void onReceiveSignalLevel(short rssi, short snr) {
            sendRxRadioLevelUpdate(rssi, snr);
        }

        @Override
        protected void onReceiveLog(String logData) {
            Log.i(TAG, "RX: " + logData);
            storeLogData(logData, false);
        }

        @Override
        protected void onTransmitPcmAudio(String src, String dst, int codec, short[] frame) {
            String note = (src == null ? "UNK" : src) + "→" + (dst == null ? "UNK" : dst);
            sendStatusUpdate(AppMessage.EV_TRANSMITTED_VOICE, note);
            sendTxAudioLevelUpdate(frame);
        }

        @Override
        protected void onTransmitCompressedAudio(String src, String dst, int codec, byte[] frame) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void onTransmitData(String src, String dst, byte[] data) {
            String note = (src == null ? "UNK" : src) + "→" + (dst == null ? "UNK" : dst);
            sendStatusUpdate(AppMessage.EV_TRANSMITTED_VOICE, note);
        }

        @Override
        protected void onTransmitLog(String logData) {
            Log.i(TAG, "TX: " + logData);
            storeLogData(logData, true);
        }

        @Override
        protected void onProtocolRxError() {
            sendStatusUpdate(AppMessage.EV_RX_ERROR, null);
            Log.e(TAG, "Protocol RX error");
        }

        @Override
        protected void onProtocolTxError() {
            sendStatusUpdate(AppMessage.EV_TX_ERROR, null);
            Log.e(TAG, "Protocol TX error");
        }
    };

    void storeLogData(String logData, boolean isTransmit) {
        // TODO, parse through aprs data
        String[] callsignData = logData.split(">");
        if (callsignData.length >= 2) {
            LogItem logItem = new LogItem();
            logItem.setTimestampEpoch(System.currentTimeMillis());
            logItem.setSrcCallsign(callsignData[0]);
            logItem.setLogLine(logData);
            logItem.setIsTransmit(isTransmit);
            _logItemRepository.insertLogItem(logItem);
        }
    }

    private void restartListening() {
        cancelListening();
        startListening();
    }

    private void startListening() {
        if (_currentStatus == AppMessage.EV_LISTENING) {
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
        sendStatusUpdate(AppMessage.EV_LISTENING, null);
    }

    private void processRecordPlaybackToggle() throws IOException {
        // playback -> recording
        if (_needTransmission && _systemAudioRecorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            _systemAudioPlayer.stop();
            _systemAudioRecorder.startRecording();
            sendRxAudioLevelUpdate(null);
        }
        // recording -> playback
        if (!_needTransmission && _systemAudioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
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
                sendStatusUpdate(AppMessage.EV_RECEIVING, null);
            }
        }
    }

    private void quitProcessing() {
        Log.i(TAG, "quitProcessing()");
        _processPeriodicTimer.cancel();
        _processPeriodicTimer.purge();
        Looper.myLooper().quitSafely();
    }

    private void onWorkerIncomingMessage(Message msg) {
        switch (AppMessage.values()[msg.what]) {
            case CMD_PROCESS:
                try {
                    processRxTx();
                } catch (IOException e) {
                    e.printStackTrace();
                    quitProcessing();
                }
                break;
            case CMD_QUIT:
                quitProcessing();
                break;
            case CMD_SEND_LOCATION_TO_TNC:
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

    private void startWorkerMessageHandler() {
        _onMessageReceived = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                onWorkerIncomingMessage(msg);
            }
        };
        _processPeriodicTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = AppMessage.CMD_PROCESS.toInt();
                _onMessageReceived.sendMessage(msg);
            }
        }, 0, PROCESS_INTERVAL_MS);
    }

    @Override
    public void run() {
        Log.i(TAG, "Starting message loop");
        setPriority(Thread.MAX_PRIORITY);
        Looper.prepare();

        sendStatusUpdate(AppMessage.EV_CONNECTED, null);
        _systemAudioPlayer.play();

        try {
            _protocol.initialize(_transport, _context, _protocolCallback);
            startWorkerMessageHandler();
            Looper.loop();
        } catch (IOException e) {
            e.printStackTrace();
        }

        cleanup();
        sendStatusUpdate(AppMessage.EV_DISCONNECTED, null);
        Log.i(TAG, "Exiting message loop");
    }
}
