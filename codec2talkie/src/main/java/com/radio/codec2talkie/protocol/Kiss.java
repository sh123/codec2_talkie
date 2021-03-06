package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Kiss implements Protocol {

    private static final String TAG = Kiss.class.getSimpleName();

    private final int INPUT_RAW_BUFFER_SIZE = 8192;
    private final int KISS_CMD_BUFFER_SIZE = 128;

    private final int KISS_TX_FRAME_MAX_SIZE = 48;

    private final int KISS_RADIO_CONTROL_COMMAND_SIZE = 17;

    private final byte KISS_FEND = (byte)0xc0;
    private final byte KISS_FESC = (byte)0xdb;
    private final byte KISS_TFEND = (byte)0xdc;
    private final byte KISS_TFESC = (byte)0xdd;

    // only port 0 is supported
    private final byte KISS_CMD_DATA = (byte)0x00;
    private final byte KISS_CMD_TX_DELAY = (byte)0x01;
    private final byte KISS_CMD_P = (byte)0x02;
    private final byte KISS_CMD_SLOT_TIME = (byte)0x03;
    private final byte KISS_CMD_TX_TAIL = (byte)0x04;
    private final byte KISS_CMD_SET_HARDWARE = (byte)0x06;
    private final byte KISS_CMD_SIGNAL_REPORT = (byte)0x07;
    private final byte KISS_CMD_NOCMD = (byte)0x80;

    private final byte CSMA_PERSISTENCE = (byte)0xff;
    private final byte CSMA_SLOT_TIME = (byte)0x00;
    private final byte TX_DELAY_10MS_UNITS = (byte)(250 / 10);
    private final byte TX_TAIL_10MS_UNITS = (byte)(500 / 10);

    private enum State {
        GET_START,
        GET_END,
        GET_CMD,
        GET_DATA,
        ESCAPE
    };

    private enum DataType {
        RAW,
        SIGNAL_REPORT
    };

    private DataType _kissDataType = DataType.RAW;
    private State _kissState = State.GET_START;

    protected final byte[] _inputRawDataBuffer;
    private final byte[] _outputKissBuffer;
    private final byte[] _inputKissBuffer;
    private final byte[] _kissCmdBuffer;

    protected Transport _transport;

    private int _outputKissBufferPos;
    private int _inputKissBufferPos;
    private int _kissCmdBufferPos;

    private SharedPreferences _sharedPreferences;
    private boolean _isExtendedMode;

    public Kiss() {
        _inputRawDataBuffer = new byte[INPUT_RAW_BUFFER_SIZE];
        _kissCmdBuffer = new byte [KISS_CMD_BUFFER_SIZE];

        _outputKissBuffer = new byte[KISS_TX_FRAME_MAX_SIZE];
        _inputKissBuffer = new byte[64 * KISS_TX_FRAME_MAX_SIZE];

        _outputKissBufferPos = 0;
        _inputKissBufferPos = 0;
        _kissCmdBufferPos = 0;

        _isExtendedMode = false;
    }

    public void initialize(Transport transport, Context context) throws IOException {
        _transport = transport;

        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        _isExtendedMode = _sharedPreferences.getBoolean(PreferenceKeys.KISS_EXTENSIONS_ENABLED, false);

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

    private void initializeExtended() throws IOException {
        /*
          struct LoraControlCommand {
            uint32_t freq;
            uint32_t bw;
            uint16_t sf;
            uint16_t cr;
            uint16_t pwr;
            uint16_t sync;
            uint8_t crc;
          } __attribute__((packed));
        */
        String freq = _sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_FREQUENCY, "433775000");
        String bw = _sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_BANDWIDTH, "125000");
        String sf = _sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_SF, "7");
        String cr = _sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_CR, "6");
        String pwr = _sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_POWER, "20");
        String sync = _sharedPreferences.getString(PreferenceKeys.KISS_EXTENSIONS_RADIO_SYNC, "34");
        byte crc = (byte)(_sharedPreferences.getBoolean(PreferenceKeys.KISS_EXTENSIONS_RADIO_CRC, true) ? 1 : 0);

        ByteBuffer rawBuffer  = ByteBuffer.allocate(KISS_RADIO_CONTROL_COMMAND_SIZE);

        rawBuffer.putInt(Integer.parseInt(freq))
                .putInt(Integer.parseInt(bw))
                .putShort(Short.parseShort(sf))
                .putShort(Short.parseShort(cr))
                .putShort(Short.parseShort(pwr))
                .putShort(Short.parseShort(sync, 16))
                .put(crc)
                .rewind();

        startKissPacket(KISS_CMD_SET_HARDWARE);
        for (byte b: rawBuffer.array()) {
            sendKissByte(b);
        }
        completeKissPacket();
    }

    public void send(byte [] frame) throws IOException {
        ByteBuffer escapedFrame = escape(frame);
        int escapedFrameSize = escapedFrame.position();
        escapedFrame.rewind();

        if (_outputKissBufferPos == 0) {
            startKissPacket(KISS_CMD_DATA);
        }
        // new frame does not fit, complete and create new frame
        if ( _outputKissBufferPos + escapedFrameSize >= KISS_TX_FRAME_MAX_SIZE) {
            completeKissPacket();
            startKissPacket(KISS_CMD_DATA);
        }
        // write new data
        while (escapedFrame.position() < escapedFrameSize) {
            sendKissByte(escapedFrame.get());
        }
    }

    public boolean receive(Callback callback) throws IOException {
        int bytesRead = _transport.read(_inputRawDataBuffer);
        if (bytesRead > 0) {
            receiveKissData(Arrays.copyOf(_inputRawDataBuffer, bytesRead), callback);
            return true;
        }
        return false;
    }

    public void flush() throws IOException{
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
            case KISS_FEND:
                break;
            default:
                Log.w(TAG, "Unsupported KISS command code: " + b);
                _kissState = State.GET_END;
                break;
        }
    }

    private void processData(byte b, Callback callback) {
        switch (b) {
            case KISS_FESC:
                _kissState = State.ESCAPE;
                break;
            case KISS_FEND:
                if (_kissDataType == DataType.RAW) {
                    callback.onReceiveAudioFrames(Arrays.copyOf(_inputKissBuffer, _inputKissBufferPos));
                } else if (_kissDataType == DataType.SIGNAL_REPORT && _isExtendedMode) {
                    callback.onReceiveSignalLevel(Arrays.copyOf(_kissCmdBuffer, _kissCmdBufferPos));
                    _kissCmdBufferPos = 0;
                }
                resetState();
                break;
            default:
                if (_kissDataType == DataType.RAW) {
                    receiveFrameByte(b);
                } else if (_kissDataType == DataType.SIGNAL_REPORT) {
                    _kissCmdBuffer[_kissCmdBufferPos++] = b;
                }
                break;
        }
    }

    protected void receiveKissData(byte[] data, Callback callback) {
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
                    processData(b, callback);
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
        _outputKissBuffer[_outputKissBufferPos++] = b;
    }

    private void receiveFrameByte(byte b) {
        _inputKissBuffer[_inputKissBufferPos++] = b;
        if (_inputKissBufferPos >= _inputKissBuffer.length) {
            Log.e(TAG, "Input KISS buffer overflow, discarding frame");
            resetState();
        }
    }

    private void resetState() {
        _kissState = State.GET_START;
        _inputKissBufferPos = 0;
    }

    private void startKissPacket(byte commandCode) {
        sendKissByte(KISS_FEND);
        sendKissByte(commandCode);
    }

    private void completeKissPacket() throws IOException {
        if (_outputKissBufferPos > 0) {
            sendKissByte(KISS_FEND);
            _transport.write(Arrays.copyOf(_outputKissBuffer, _outputKissBufferPos));
            _outputKissBufferPos = 0;
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
