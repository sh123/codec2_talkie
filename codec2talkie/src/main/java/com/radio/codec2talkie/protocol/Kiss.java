package com.radio.codec2talkie.protocol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.settings.SettingsWrapper;
import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Kiss implements Protocol {

    private static final String TAG = Kiss.class.getSimpleName();

    private static final int TRANSPORT_OUTPUT_BUFFER_SIZE = 1024;
    private static final int TRANSPORT_INPUT_BUFFER_SIZE = 1024;
    private static final int FRAME_OUTPUT_BUFFER_SIZE = 1024;
    private static final int KISS_CMD_BUFFER_SIZE = 128;

    private static final int KISS_RADIO_CONTROL_COMMAND_SIZE = 34;

    private static final byte KISS_FEND = (byte)0xc0;
    private static final byte KISS_FESC = (byte)0xdb;
    private static final byte KISS_TFEND = (byte)0xdc;
    private static final byte KISS_TFESC = (byte)0xdd;

    // only port 0 is supported
    private static final byte KISS_CMD_DATA = (byte)0x00;
    private static final byte KISS_CMD_TX_DELAY = (byte)0x01;
    private static final byte KISS_CMD_P = (byte)0x02;
    private static final byte KISS_CMD_SLOT_TIME = (byte)0x03;
    private static final byte KISS_CMD_TX_TAIL = (byte)0x04;
    private static final byte KISS_CMD_SET_HARDWARE = (byte)0x06;
    private static final byte KISS_CMD_SIGNAL_REPORT = (byte)0x07;
    private static final byte KISS_CMD_REBOOT = (byte)0x08;
    private static final byte KISS_CMD_TELEMETRY = (byte)0x09;
    private static final byte KISS_CMD_NOCMD = (byte)0x80;

    private static final byte CSMA_PERSISTENCE = (byte)0xff;
    private static final byte CSMA_SLOT_TIME = (byte)0x00;
    private static final byte TX_DELAY_10MS_UNITS = (byte)(250 / 10);
    private static final byte TX_TAIL_10MS_UNITS = (byte)(500 / 10);

    private static final int SIGNAL_LEVEL_EVENT_SIZE = 4;
    private static final int TELEMETRY_EVENT_SIZE = 2;

    private enum State {
        GET_START,
        GET_END,
        GET_CMD,
        GET_DATA,
        ESCAPE
    }

    private enum DataType {
        RAW,
        SIGNAL_REPORT,
        TELEMETRY
    }

    private DataType _kissDataType = DataType.RAW;
    private State _kissState = State.GET_START;

    protected final byte[] _transportInputBuffer;

    private int _transportOutputBufferPos;
    private final byte[] _transportOutputBuffer;

    private int _frameOutputBufferPos;
    private final byte[] _frameOutputBuffer;

    private int _kissCmdBufferPos;
    private final byte[] _kissCmdBuffer;

    protected Transport _transport;

    private SharedPreferences _sharedPreferences;
    private boolean _isExtendedMode;

    private Context _context;

    private ProtocolCallback _parentProtocolCallback;

    public Kiss() {
        _transportInputBuffer = new byte[TRANSPORT_INPUT_BUFFER_SIZE];
        _transportOutputBuffer = new byte[TRANSPORT_OUTPUT_BUFFER_SIZE];

        _kissCmdBuffer = new byte [KISS_CMD_BUFFER_SIZE];

        _frameOutputBuffer = new byte[FRAME_OUTPUT_BUFFER_SIZE];

        _transportOutputBufferPos = 0;
        _frameOutputBufferPos = 0;
        _kissCmdBufferPos = 0;

        _isExtendedMode = false;
    }

    @Override
    public void initialize(Transport transport, Context context, ProtocolCallback protocolCallback) throws IOException {

        Log.i(TAG, "Initializing " + transport.toString());

        _parentProtocolCallback = protocolCallback;
        _transport = transport;
        _context = context;

        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        _isExtendedMode = SettingsWrapper.isKissExtensionEnabled(_sharedPreferences);

        byte tncCsmaPersistence = (byte) Integer.parseInt(_sharedPreferences.getString(PreferenceKeys.KISS_BASIC_P, String.valueOf(CSMA_PERSISTENCE)));
        byte tncCsmaSlotTime = (byte) Integer.parseInt(_sharedPreferences.getString(PreferenceKeys.KISS_BASIC_SLOT_TIME, String.valueOf(CSMA_SLOT_TIME)));
        byte tncTxDelay = (byte) Integer.parseInt(_sharedPreferences.getString(PreferenceKeys.KISS_BASIC_TX_DELAY, String.valueOf(TX_DELAY_10MS_UNITS)));
        byte tncTxTail = (byte) Integer.parseInt(_sharedPreferences.getString(PreferenceKeys.KISS_BASIC_TX_TAIL, String.valueOf(TX_TAIL_10MS_UNITS)));

        startKissPacket(KISS_CMD_P);
        sendKissByte(tncCsmaPersistence);
        completeKissPacket();

        startKissPacket(KISS_CMD_SLOT_TIME);
        sendKissByte(tncCsmaSlotTime);
        completeKissPacket();

        startKissPacket(KISS_CMD_TX_DELAY);
        sendKissByte(tncTxDelay);
        completeKissPacket();

        startKissPacket(KISS_CMD_TX_TAIL);
        sendKissByte(tncTxTail);
        completeKissPacket();

        if (_isExtendedMode) {
            initializeExtended();
        }
    }

    @Override
    public int getPcmAudioRecordBufferSize() {
        Log.w(TAG, "getPcmAudioBufferSize() is not supported");
        return -1;
    }

    private void initializeExtended() throws IOException {
        /*
        struct SetHardware {
            uint32_t freqRx;
            uint32_t freqTx;
            uint8_t modType;
            uint16_t pwr;
            uint32_t bw;
            uint16_t sf;
            uint16_t cr;
            uint16_t sync;
            uint8_t crc;
            uint32_t fskBitRate;
            uint32_t fskFreqDev;
            uint32_t fskRxBw;
        } __attribute__((packed));
        */
        String freq = _sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_FREQUENCY, "433775000");
        String freqTx = _sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_FREQUENCY_TX, "433775000");
        if (!_sharedPreferences.getBoolean(PreferenceKeys.KISS_EXTENSIONS_RADIO_SPLIT_FREQ, false)) freqTx = freq;
        String modType = _sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_MOD, "0");
        String bw = _sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_BANDWIDTH, "125000");
        String sf = _sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_SF, "7");
        String cr = _sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_CR, "6");
        String pwr = _sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_POWER, "20");
        String sync = _sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_SYNC, "34");
        byte crc = (byte)(_sharedPreferences.getBoolean(PreferenceKeys.KISS_EXTENSIONS_RADIO_CRC, true) ? 1 : 0);
        String fskBitRate = _sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_FSK_BIT_RATE, "4800");
        String fskFreqDev = _sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_FSK_FREQ_DEV, "1200");
        String fskRxBw = _sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_FSK_RX_BW, "9700");

        ByteBuffer rawBuffer  = ByteBuffer.allocate(KISS_RADIO_CONTROL_COMMAND_SIZE);

        rawBuffer.putInt(Integer.parseInt(freq))
                .putInt(Integer.parseInt(freqTx))
                .put(Byte.parseByte(modType))
                .putShort(Short.parseShort(pwr))
                .putInt(Integer.parseInt(bw))
                .putShort(Short.parseShort(sf))
                .putShort(Short.parseShort(cr))
                .putShort(Short.parseShort(sync, 16))
                .put(crc)
                .putInt(Integer.parseInt(fskBitRate))
                .putInt(Integer.parseInt(fskFreqDev))
                .putInt(Integer.parseInt(fskRxBw))
                .rewind();

        send(KISS_CMD_SET_HARDWARE, rawBuffer.array());
        ContextCompat.registerReceiver(_context, onModemRebootRequested,
                new IntentFilter(PreferenceKeys.KISS_EXTENSIONS_ACTION_REBOOT_REQUESTED),
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    public final BroadcastReceiver onModemRebootRequested = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(_context, R.string.kiss_toast_modem_reboot, Toast.LENGTH_SHORT).show();
            startKissPacket(KISS_CMD_REBOOT);
            try {
                completeKissPacket();
            } catch (IOException e) {
                e.printStackTrace();
                resetState();
            }
        }
    };

    @Override
    public void sendCompressedAudio(String src, String dst, byte[] frame) throws IOException {
        // NOTE, KISS does not distinguish between audio and data packet, upper layer should decide
        send(KISS_CMD_DATA, frame);
    }

    @Override
    public void sendTextMessage(TextMessage textMessage) throws IOException {
        Log.w(TAG, "sendTextMessage() is not supported");
    }

    @Override
    public void sendPcmAudio(String src, String dst, short[] pcmFrame)  {
        Log.w(TAG, "sendPcmAudio() is not supported");
    }

    @Override
    public void sendData(String src, String dst, String path, byte[] dataPacket) throws IOException {
        // NOTE, KISS does not distinguish between audio and data packet, upper layer should decide
        send(KISS_CMD_DATA, dataPacket);
    }

    @Override
    public boolean receive() throws IOException {
        int bytesRead = _transport.read(_transportInputBuffer);
        if (bytesRead > 0) {
            receiveKissData(Arrays.copyOf(_transportInputBuffer, bytesRead), _parentProtocolCallback);
            return true;
        }
        return false;
    }

    @Override
    public void sendPosition(Position position) {
        Log.w(TAG, "sendPosition() is not supported");
    }

    @Override
    public void flush() throws IOException{
        completeKissPacket();
    }

    @Override
    public void close() {
        try {
            _context.unregisterReceiver(onModemRebootRequested);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private void send(byte commandCode, byte[] data) throws IOException {
        // escape
        ByteBuffer escapedFrame = escape(data);
        int escapedFrameSize = escapedFrame.position();
        escapedFrame.rewind();

        // send
        startKissPacket(commandCode);
        while (escapedFrame.position() < escapedFrameSize) {
            sendKissByte(escapedFrame.get());
        }
        completeKissPacket();
    }

    private void processCommand(byte b) {
        switch (b) {
            case KISS_CMD_DATA:
                _kissState = State.GET_DATA;
                _kissDataType = DataType.RAW;
                break;
            case KISS_CMD_SIGNAL_REPORT:
                _kissState = State.GET_DATA;
                _kissDataType = DataType.SIGNAL_REPORT;
                _kissCmdBufferPos = 0;
                break;
            case KISS_CMD_TELEMETRY:
                _kissState = State.GET_DATA;
                _kissDataType = DataType.TELEMETRY;
                _kissCmdBufferPos = 0;
                break;
            case KISS_FEND:
                break;
            default:
                Log.w(TAG, "Unsupported KISS command code: " + b);
                _kissState = State.GET_END;
                break;
        }
    }

    private void processData(byte b, ProtocolCallback protocolCallback) {
        switch (b) {
            case KISS_FESC:
                _kissState = State.ESCAPE;
                break;
            case KISS_FEND:
                if (_kissDataType == DataType.RAW) {
                    // NOTE, default data is compressed audio, KISS does not distinguish between audio and data packets, upper layer should decide
                    protocolCallback.onReceiveCompressedAudio(null, null, Arrays.copyOf(_frameOutputBuffer, _frameOutputBufferPos));
                } else if (_kissDataType == DataType.SIGNAL_REPORT && _isExtendedMode) {
                    byte[] signalLevelRaw = Arrays.copyOf(_kissCmdBuffer, _kissCmdBufferPos);
                    ByteBuffer data = ByteBuffer.wrap(signalLevelRaw);
                    if (signalLevelRaw.length == SIGNAL_LEVEL_EVENT_SIZE) {
                        short rssi = data.getShort();
                        short snr = data.getShort();
                        protocolCallback.onReceiveSignalLevel(rssi, snr);
                    } else {
                        protocolCallback.onProtocolRxError();
                        Log.e(TAG, "Signal event of wrong size");
                    }
                    _kissCmdBufferPos = 0;
                } else if (_kissDataType == DataType.TELEMETRY && _isExtendedMode) {
                    byte[] telemetryRaw = Arrays.copyOf(_kissCmdBuffer, _kissCmdBufferPos);
                    ByteBuffer data = ByteBuffer.wrap(telemetryRaw);
                    if (telemetryRaw.length == TELEMETRY_EVENT_SIZE) {
                        short batVoltage = data.getShort();
                        protocolCallback.onReceiveTelemetry(batVoltage);
                    } else {
                        protocolCallback.onProtocolRxError();
                        Log.e(TAG, "Telemetry event of wrong size " + telemetryRaw.length + " vs " + TELEMETRY_EVENT_SIZE);
                    }
                    _kissCmdBufferPos = 0;
                }
                resetState();
                break;
            default:
                if (_kissDataType == DataType.RAW) {
                    receiveFrameByte(b);
                } else if (_kissDataType == DataType.SIGNAL_REPORT || _kissDataType == DataType.TELEMETRY) {
                    _kissCmdBuffer[_kissCmdBufferPos++] = b;
                }
                break;
        }
    }

    protected void receiveKissData(byte[] data, ProtocolCallback protocolCallback) {
        for (byte b : data) {
            switch (_kissState) {
                case GET_START:
                    if (b == KISS_FEND) {
                        _kissState = State.GET_CMD;
                    }
                    break;
                case GET_END:
                    if (b == KISS_FEND) {
                        resetState();
                    }
                    break;
                case GET_CMD:
                    processCommand(b);
                    break;
                case GET_DATA:
                    processData(b, protocolCallback);
                    break;
                case ESCAPE:
                    if (b == KISS_TFEND) {
                        receiveFrameByte(KISS_FEND);
                        _kissState = State.GET_DATA;
                    } else if (b == KISS_TFESC) {
                        receiveFrameByte(KISS_FESC);
                        _kissState = State.GET_DATA;
                    }
                    else if (b != KISS_FEND) {
                        Log.w(TAG, "Unknown KISS escape code: " + b);
                        _kissState = State.GET_END;
                    }
                    break;
                default:
                    Log.w(TAG, "Unknown state: " + _kissState);
                    resetState();
                    break;
            }
        }
    }

    private void sendKissByte(byte b) {
        if (_transportOutputBufferPos >= _transportOutputBuffer.length) {
            Log.e(TAG, "Output KISS buffer overflow, discarding frame");
            _transportOutputBufferPos = 0;
        }
        _transportOutputBuffer[_transportOutputBufferPos++] = b;
    }

    private void receiveFrameByte(byte b) {
        if (_frameOutputBufferPos >= _frameOutputBuffer.length) {
            Log.e(TAG, "Input KISS buffer overflow, discarding frame");
            resetState();
        }
        _frameOutputBuffer[_frameOutputBufferPos++] = b;
    }

    private void resetState() {
        _kissState = State.GET_START;
        _frameOutputBufferPos = 0;
    }

    private void startKissPacket(byte commandCode) {
        sendKissByte(KISS_FEND);
        sendKissByte(commandCode);
    }

    private void completeKissPacket() throws IOException {
        if (_transportOutputBufferPos > 0) {
            sendKissByte(KISS_FEND);
            //byte[] d = Arrays.copyOf(_transportOutputBuffer, _transportOutputBufferPos);
            //Log.i(TAG, DebugTools.bytesToHex(d));
            _transport.write(Arrays.copyOf(_transportOutputBuffer, _transportOutputBufferPos));
            _transportOutputBufferPos = 0;
        }
    }

    private ByteBuffer escape(byte [] inputBuffer) {
        ByteBuffer escapedBuffer = ByteBuffer.allocate(4 * inputBuffer.length);
        for (byte b : inputBuffer) {
            switch (b) {
                case KISS_FEND:
                    escapedBuffer.put(KISS_FESC).put(KISS_TFEND);
                    break;
                case KISS_FESC:
                    escapedBuffer.put(KISS_FESC).put(KISS_TFESC);
                    break;
                default:
                    escapedBuffer.put(b);
                    break;
            }
        }
        return escapedBuffer;
    }
}
