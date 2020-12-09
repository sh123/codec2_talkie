package com.radio.codec2talkie.kiss;

import java.io.IOException;
import java.nio.ByteBuffer;

public class KissProcessor {

    private final int KISS_FRAME_MAX_SIZE = 32;

    private final byte KISS_FEND = (byte)0xc0;
    private final byte KISS_FESC = (byte)0xdb;
    private final byte KISS_TFEND = (byte)0xdc;
    private final byte KISS_TFESC = (byte)0xdd;

    private final byte KISS_CMD_DATA = (byte)0x00;
    private final byte KISS_CMD_P = (byte)0x02;
    private final byte KISS_CMD_SLOT_TIME = (byte)0x03;
    private final byte KISS_CMD_NOCMD = (byte)0x80;

    private enum KissState {
        VOID,
        GET_CMD,
        GET_DATA,
        ESCAPE
    };

    private KissState _kissState = KissState.VOID;
    private byte _kissCmd = KISS_CMD_NOCMD;

    private final int _frameSize;
    private final byte _csmaPersistence;
    private final byte _csmaSlotTime;

    private final byte[] _inputFrameBuffer;

    private final KissCallback _callback;

    private int _outputFramePos;
    private int _inputFramePos;

    public KissProcessor(int frameSize, byte csmaPersistence, byte csmaSlotTime, KissCallback callback) {
        _frameSize = frameSize;
        _callback = callback;
        _inputFrameBuffer = new byte[frameSize];
        _csmaPersistence = csmaPersistence;
        _csmaSlotTime = csmaSlotTime;
    }

    public void setupCsma() throws IOException {
        _callback.sendByte(KISS_FEND);
        _callback.sendByte(KISS_CMD_P);
        _callback.sendByte(_csmaPersistence);
        _callback.sendByte(KISS_FEND);

        _callback.sendByte(KISS_FEND);
        _callback.sendByte(KISS_CMD_SLOT_TIME);
        _callback.sendByte(_csmaSlotTime);
        _callback.sendByte(KISS_FEND);
    }

    public void sendFrame(byte [] frame) throws IOException {
        ByteBuffer escapedBuffer = escape(frame);
        int numItems = escapedBuffer.position();
        escapedBuffer.rewind();

        if (_outputFramePos == 0) {
            startFrame();
        }
        // new data does not fit, complete and create new frame
        if (numItems + _outputFramePos >= KISS_FRAME_MAX_SIZE) {
            completeFrame();
            startFrame();
        }
        // write new data
        while (escapedBuffer.position() < numItems) {
            send(escapedBuffer.get());
        }
    }

    public void receiveByte(byte b) {
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
                    resetState();
                }
                break;
            case GET_DATA:
                if (b == KISS_FESC) {
                    _kissState = KissState.ESCAPE;
                } else if (b == KISS_FEND) {
                    if (_kissCmd == KISS_CMD_DATA) {
                        // end of packet
                    }
                    resetState();
                } else {
                    receive(b);
                }
                break;
            case ESCAPE:
                if (b == KISS_TFEND) {
                    receive(KISS_FEND);
                    _kissState = KissState.GET_DATA;
                } else if (b == KISS_TFESC) {
                    receive(KISS_FESC);
                    _kissState = KissState.GET_DATA;
                } else {
                    resetState();
                }
                break;
            default:
                break;
        }
        if (_inputFramePos >= _frameSize) {
            _callback.receiveFrame(_inputFrameBuffer);
            _inputFramePos = 0;
        }
    }

    public void flush() throws IOException{
        completeFrame();
    }

    private void send(byte b) throws IOException {
        _callback.sendByte(b);
        _outputFramePos++;
    }

    private void receive(byte b) {
        _inputFrameBuffer[_inputFramePos] = b;
        _inputFramePos++;
    }

    private void resetState() {
        _kissCmd = KISS_CMD_NOCMD;
        _kissState = KissState.VOID;
    }

    private void startFrame() throws IOException {
        send(KISS_FEND);
        send(KISS_CMD_DATA);
    }

    private void completeFrame() throws IOException {
        if (_outputFramePos > 0) {
            send(KISS_FEND);
            _outputFramePos = 0;
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
