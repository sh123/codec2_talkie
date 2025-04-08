package com.radio.codec2talkie.app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.io.IOException;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import com.radio.codec2talkie.protocol.aprs.tools.AprsIsData;
import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.storage.log.LogItem;
import com.radio.codec2talkie.storage.log.LogItemRepository;
import com.radio.codec2talkie.protocol.ProtocolCallback;
import com.radio.codec2talkie.protocol.Protocol;
import com.radio.codec2talkie.protocol.ProtocolFactory;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.storage.message.MessageItemRepository;
import com.radio.codec2talkie.storage.position.PositionItemRepository;
import com.radio.codec2talkie.storage.station.StationItemRepository;
import com.radio.codec2talkie.tools.AudioTools;
import com.radio.codec2talkie.transport.Transport;
import com.radio.codec2talkie.transport.TransportFactory;

public class AppWorker extends Thread {

    private static final String TAG = AppWorker.class.getSimpleName();

    private static final int AUDIO_MIN_LEVEL = -70;
    private static final int AUDIO_MAX_LEVEL = 0;
    private static final int AUDIO_SAMPLE_SIZE = 8000;

    private static final int PROCESS_INTERVAL_MS = 10;
    private static final int LISTEN_AFTER_MS = 1500;

    private boolean _needTransmission = false;
    private AppMessage _currentStatus = AppMessage.EV_DISCONNECTED;

    private final Protocol _protocol;
    private final Transport _transport;

    // input data, bt -> audio
    private AudioTrack _systemAudioPlayer;

    // output data., mic -> bt
    private AudioRecord _systemAudioRecorder;
    private short[] _recordAudioBuffer;

    // callbacks
    private final Handler _onWorkerStateChanged;
    private Handler _onMessageReceived;
    private final Timer _processPeriodicTimer;

    // listen timer
    private Timer _listenTimer;

    // storage integration
    private final LogItemRepository _logItemRepository;
    private final MessageItemRepository _messageItemRepository;
    private final PositionItemRepository _positionItemRepository;
    private final StationItemRepository _stationItemRepository;

    private final Context _context;
    private final SharedPreferences _sharedPreferences;

    public AppWorker(TransportFactory.TransportType transportType,
                     Handler onWorkerStateChanged, Context context) throws IOException {
        _onWorkerStateChanged = onWorkerStateChanged;

        _context = context;
        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_context);

        _logItemRepository = new LogItemRepository((Application)context);
        _messageItemRepository = new MessageItemRepository((Application)context);
        _positionItemRepository = new PositionItemRepository((Application)context);
        _stationItemRepository = new StationItemRepository((Application)context);

        _transport = TransportFactory.create(transportType, context);
        _protocol = ProtocolFactory.create(context);

        _processPeriodicTimer = new Timer();

        int audioSource = Integer.parseInt(_sharedPreferences.getString(PreferenceKeys.APP_AUDIO_SOURCE, "6"));
        int audioDestination = Integer.parseInt(_sharedPreferences.getString(PreferenceKeys.APP_AUDIO_DESTINATION, "1"));
        constructSystemAudioDevices(transportType, audioSource, audioDestination);
    }

    private void constructSystemAudioDevices(TransportFactory.TransportType transportType, int audioSource, int audioDestination) {
        int _audioRecorderMinBufferSize = AudioRecord.getMinBufferSize(
                AUDIO_SAMPLE_SIZE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
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
        _systemAudioPlayer = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(audioDestination)
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

        // Use built in mic and speaker for speech when sound modem is in use
        if (transportType == TransportFactory.TransportType.SOUND_MODEM) {
            selectBuiltinMicAndSpeakerEarpiece(audioDestination != AudioAttributes.USAGE_VOICE_COMMUNICATION);
        }
    }

    private void selectBuiltinMicAndSpeakerEarpiece(boolean isSpeakerOutput) {
        AudioManager audioManager = (AudioManager)_context.getSystemService(Context.AUDIO_SERVICE);

        for (AudioDeviceInfo inputDevice : audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)) {
            boolean isBuiltIn = inputDevice.getType() == AudioDeviceInfo.TYPE_BUILTIN_MIC;
            Log.i(TAG, "input device: " + isBuiltIn + " " + inputDevice.getProductName() + " " + inputDevice.getType());
            if (isBuiltIn) {
                boolean isSet = _systemAudioRecorder.setPreferredDevice(inputDevice);
                if (!isSet)
                    Log.w(TAG, "cannot select input " + inputDevice.getProductName());
                break;
            }
        }

        for (AudioDeviceInfo outputDevice : audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            boolean isBuiltIn = outputDevice.getType() == (isSpeakerOutput ? AudioDeviceInfo.TYPE_BUILTIN_SPEAKER : AudioDeviceInfo.TYPE_BUILTIN_EARPIECE);
            Log.i(TAG, "output device: " + isBuiltIn + " " + outputDevice.getProductName() + " " + outputDevice.getType());
            if (isBuiltIn) {
                boolean isSet = _systemAudioPlayer.setPreferredDevice(outputDevice);
                if (!isSet)
                    Log.w(TAG, "cannot select output " + outputDevice.getProductName());
                break;
            }
        }
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
        Log.i(TAG, String.format("Position sent: %s, lat: %f, lon: %f, course: %f, speed: %f, alt: %f",
                position.maidenHead, position.latitude, position.longitude,
                position.bearingDegrees, position.speedMetersPerSecond, position.altitudeMeters));
        _onMessageReceived.sendMessage(msg);
    }

    public void sendTextMessage(TextMessage textMessage) {
        Message msg = Message.obtain();
        msg.what = AppMessage.CMD_SEND_MESSAGE.toInt();
        msg.obj = textMessage;
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
            _onWorkerStateChanged.sendMessage(msg);
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
        _onWorkerStateChanged.sendMessage(msg);
    }

    private void sendTelemetryUpdate(int batVoltage) {
        Message msg = Message.obtain();
        msg.what = AppMessage.EV_TELEMETRY.toInt();
        msg.arg1 = batVoltage;
        _onWorkerStateChanged.sendMessage(msg);
    }
    private void sendRxAudioLevelUpdate(short [] pcmAudioSamples) {
        Message msg = Message.obtain();
        msg.what = AppMessage.EV_RX_LEVEL.toInt();
        msg.arg1 = AudioTools.getSampleLevelDb(pcmAudioSamples);
        _onWorkerStateChanged.sendMessage(msg);
    }

    private void sendTxAudioLevelUpdate(short [] pcmAudioSamples) {
        Message msg = Message.obtain();
        msg.what = AppMessage.EV_TX_LEVEL.toInt();
        msg.arg1 = AudioTools.getSampleLevelDb(pcmAudioSamples);
        _onWorkerStateChanged.sendMessage(msg);
    }

    private void recordAndSendAudioFrame() throws IOException {
        _systemAudioRecorder.read(_recordAudioBuffer, 0, _recordAudioBuffer.length);
        _protocol.sendPcmAudio(null, null, _recordAudioBuffer);
    }

    private final ProtocolCallback _protocolCallback = new ProtocolCallback() {
        @Override
        protected void onReceivePosition(Position position) {
            Log.i(TAG, String.format("Position received: %s→%s, %s, lat: %f, lon: %f, course: %f, speed: %f, alt: %f, sym: %s, range: %.2f, status: %s, comment: %s",
                    position.srcCallsign, position.dstCallsign, position.maidenHead, position.latitude, position.longitude,
                    position.bearingDegrees, position.speedMetersPerSecond, position.altitudeMeters,
                    position.symbolCode, position.rangeMiles, position.status, position.comment));
            _positionItemRepository.upsertPositionItem(position.toPositionItem(false));
            _stationItemRepository.upsertStationItem(position.toStationItem());

            String note = (position.srcCallsign == null ? "UNK" : position.srcCallsign) + "→" +
                    (position.dstCallsign == null ? "UNK" : position.dstCallsign);
            sendStatusUpdate(AppMessage.EV_POSITION_RECEIVED, note);
        }

        @Override
        protected void onReceivePcmAudio(String src, String dst, short[] pcmFrame) {
            String note = (src == null ? "UNK" : src) + "→" + (dst == null ? "UNK" : dst);
            sendStatusUpdate(AppMessage.EV_VOICE_RECEIVED, note);
            sendRxAudioLevelUpdate(pcmFrame);
            if (_systemAudioPlayer.getPlayState() != AudioTrack.PLAYSTATE_PLAYING)
                _systemAudioPlayer.play();
            _systemAudioPlayer.write(pcmFrame, 0, pcmFrame.length);
            _systemAudioPlayer.stop();
        }

        @Override
        protected void onReceiveCompressedAudio(String src, String dst, byte[] audioFrame) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void onReceiveTextMessage(TextMessage textMessage) {
            String note = (textMessage.src == null ? "UNK" : textMessage.src) + "→" +
                    (textMessage.dst == null ? "UNK" : textMessage.dst);
            sendStatusUpdate(AppMessage.EV_TEXT_MESSAGE_RECEIVED, note + ": " + textMessage.text);
            if (textMessage.isAutoReply()) {
                // TODO, acknowledge or reject message with the given (src, dst, ackId)
            } else {
                _messageItemRepository.insertMessageItem(textMessage.toMessageItem(false));
            }
            Log.i(TAG, "message received: " + textMessage.text);
        }

        @Override
        protected void onReceiveData(String src, String dst, String path, byte[] data) {
            String note = (src == null ? "UNK" : src) + "→" + (dst == null ? "UNK" : dst);
            sendStatusUpdate(AppMessage.EV_DATA_RECEIVED, note);
        }

        @Override
        protected void onReceiveSignalLevel(short rssi, short snr) {
            sendRxRadioLevelUpdate(rssi, snr);
        }

        @Override
        protected void onReceiveTelemetry(int batVoltage) {
            sendTelemetryUpdate(batVoltage);
        }

        @Override
        protected void onReceiveLog(String logData) {
            Log.i(TAG, "RX-LOG: " + logData);
            storeLogData(logData, false);
        }

        @Override
        protected void onTransmitPcmAudio(String src, String dst, short[] frame) {
            String note = (src == null ? "UNK" : src) + "→" + (dst == null ? "UNK" : dst);
            sendStatusUpdate(AppMessage.EV_TRANSMITTED_VOICE, note);
            sendTxAudioLevelUpdate(frame);
        }

        @Override
        protected void onTransmitCompressedAudio(String src, String dst, byte[] frame) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void onTransmitTextMessage(TextMessage textMessage) {
            String note = (textMessage.src == null ? "UNK" : textMessage.src) + "→" +
                    (textMessage.dst == null ? "UNK" : textMessage.dst);
            sendStatusUpdate(AppMessage.EV_TEXT_MESSAGE_TRANSMITTED, note);
            if (!textMessage.isAutoReply()) {
                _messageItemRepository.insertMessageItem(textMessage.toMessageItem(true));
            }
        }

        @Override
        protected void onTransmitPosition(Position position) {
            _positionItemRepository.upsertPositionItem(position.toPositionItem(true));
            _stationItemRepository.upsertStationItem(position.toStationItem());
        }

        @Override
        protected void onTransmitData(String src, String dst, String path, byte[] data) {
            String note = (src == null ? "UNK" : src) + "→" + (dst == null ? "UNK" : dst);
            sendStatusUpdate(AppMessage.EV_TRANSMITTED_VOICE, note);
        }

        @Override
        protected void onTransmitLog(String logData) {
            Log.i(TAG, "TX-LOG: " + logData);
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

    private void storeLogData(String logData, boolean isTransmit) {
        AprsIsData aprsIsData = AprsIsData.fromString(logData);
        if (aprsIsData != null) {
            LogItem logItem = new LogItem();
            logItem.setTimestampEpoch(System.currentTimeMillis());
            logItem.setIsTransmit(isTransmit);
            logItem.setSrcCallsign(aprsIsData.src);
            logItem.setLogLine(logData);
            _logItemRepository.insertLogItem(logItem);
            _stationItemRepository.upsertStationItem(logItem.toStationItem());
            if (aprsIsData.hasThirdParty()) {
                LogItem logItemThirdParty = new LogItem();
                logItemThirdParty.setTimestampEpoch(System.currentTimeMillis());
                logItemThirdParty.setIsTransmit(isTransmit);
                logItemThirdParty.setSrcCallsign(aprsIsData.thirdParty.src);
                logItemThirdParty.setLogLine(aprsIsData.thirdParty.convertToString(true));
                _logItemRepository.insertLogItem(logItemThirdParty);
                _stationItemRepository.upsertStationItem(logItemThirdParty.toStationItem());
            }
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
            sendTxAudioLevelUpdate(null);
        }
    }

    private void cleanup() {
        Log.i(TAG, "cleanup() started");
        try {
            _systemAudioRecorder.stop();
        } catch (IllegalStateException ignored) {}
        _systemAudioRecorder.release();

        try {
            _systemAudioPlayer.stop();
        } catch (IllegalStateException ignored) {}
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
        Objects.requireNonNull(Looper.myLooper()).quitSafely();
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
            case CMD_SEND_MESSAGE:
                TextMessage textMessage = (TextMessage) msg.obj;
                try {
                    _protocol.sendTextMessage(textMessage);
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
        _onMessageReceived = new Handler(Objects.requireNonNull(Looper.myLooper())) {
            @Override
            public void handleMessage(@NonNull Message msg) {
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
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
        Looper.prepare();

        sendStatusUpdate(AppMessage.EV_CONNECTED, null);
        _systemAudioPlayer.play();

        try {
            _protocol.initialize(_transport, _context, _protocolCallback);
            _recordAudioBuffer = new short[_protocol.getPcmAudioRecordBufferSize()];
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
