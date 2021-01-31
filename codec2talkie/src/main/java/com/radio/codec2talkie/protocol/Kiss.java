package com.radio.codec2talkie.protocol;

import android.util.Log;

import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Kiss implements Protocol {

    private static final String TAG = Kiss.class.getSimpleName();

    private final int RX_BUFFER_SIZE = 8192;

    private final int KISS_TX_FRAME_MAX_SIZE = 48;

    private final byte KISS_FEND = (byte)0xc0;
    private final byte KISS_FESC = (byte)0xdb;
    private final byte KISS_TFEND = (byte)0xdc;
    private final byte KISS_TFESC = (byte)0xdd;

    private final byte KISS_CMD_DATA = (byte)0x00;
    private final byte KISS_CMD_TX_DELAY = (byte)0x01;
    private final byte KISS_CMD_P = (byte)0x02;
    private final byte KISS_CMD_SLOT_TIME = (byte)0x03;
    private final byte KISS_CMD_TX_TAIL = (byte)0x04;
    private final byte KISS_CMD_NOCMD = (byte)0x80;

    private final byte CSMA_PERSISTENCE = (byte)0xff;
    private final byte CSMA_SLOT_TIME = (byte)0x00;
    private final byte TX_DELAY_10MS_UNITS = (byte)(250 / 10);
    private final byte TX_TAIL_10MS_UNITS = (byte)(500 / 10);

    private enum KissState {
        VOID,
        GET_CMD,
        GET_DATA,
        ESCAPE
    };

    private KissState _kissState = KissState.VOID;
    private byte _kissCmd = KISS_CMD_NOCMD;

    private final byte _tncCsmaPersistence;
    private final byte _tncCsmaSlotTime;
    private final byte _tncTxDelay;
    private final byte _tncTxTail;

    protected final byte[] _rxDataBuffer;
    private final byte[] _outputKissBuffer;
    private final byte[] _inputKissBuffer;

    protected Transport _transport;

    private int _outputKissBufferPos;
    private int _inputKissBufferPos;

    public Kiss() {
        _rxDataBuffer = new byte[RX_BUFFER_SIZE];

        _outputKissBuffer = new byte[KISS_TX_FRAME_MAX_SIZE];
        _inputKissBuffer = new byte[100 * KISS_TX_FRAME_MAX_SIZE];

        _tncCsmaPersistence = CSMA_PERSISTENCE;
        _tncCsmaSlotTime = CSMA_SLOT_TIME;
        _tncTxDelay = TX_DELAY_10MS_UNITS;
        _tncTxTail = TX_TAIL_10MS_UNITS;

        _outputKissBufferPos = 0;
        _inputKissBufferPos = 0;
    }

    public void initialize(Transport transport) throws IOException {
        _transport = transport;

        startKissPacket(KISS_CMD_P);
        sendKissByte(_tncCsmaPersistence);
        completeKissPacket();

        startKissPacket(KISS_CMD_SLOT_TIME);
        sendKissByte(_tncCsmaSlotTime);
        completeKissPacket();

        startKissPacket(KISS_CMD_TX_DELAY);
        sendKissByte(_tncTxDelay);
        completeKissPacket();

        startKissPacket(KISS_CMD_TX_TAIL);
        sendKissByte(_tncTxTail);
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
        int bytesRead = _transport.read(_rxDataBuffer);
        if (bytesRead > 0) {
            receiveKissData(Arrays.copyOf(_rxDataBuffer, bytesRead), callback);
            return true;
        }
        return false;
    }

    public void flush() throws IOException{
        completeKissPacket();
    }

    protected void receiveKissData(byte[] data, Callback callback) {
        for (byte b : data) {
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
                        Log.w(TAG, "Unsupported KISS command code: " + b);
                        resetState();
                    }
                    break;
                case GET_DATA:
                    if (b == KISS_FESC) {
                        _kissState = KissState.ESCAPE;
                    } else if (b == KISS_FEND) {
                        if (_kissCmd == KISS_CMD_DATA) {
                            callback.onReceiveAudioFrames(Arrays.copyOf(_inputKissBuffer, _inputKissBufferPos));
                        }
                        resetState();
                    } else {
                        receiveFrameByte(b);
                    }
                    break;
                case ESCAPE:
                    if (b == KISS_TFEND) {
                        receiveFrameByte(KISS_FEND);
                        _kissState = KissState.GET_DATA;
                    } else if (b == KISS_TFESC) {
                        receiveFrameByte(KISS_FESC);
                        _kissState = KissState.GET_DATA;
                    } else {
                        Log.w(TAG, "Unknown KISS escape code: " + b);
                        resetState();
                    }
                    break;
                default:
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
        _kissCmd = KISS_CMD_NOCMD;
        _kissState = KissState.VOID;
        _inputKissBufferPos = 0;
    }

    private void startKissPacket(byte commandCode) throws IOException {
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
