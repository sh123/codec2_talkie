package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Debug;
import android.util.Log;

import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.tools.BitTools;
import com.radio.codec2talkie.tools.ChecksumTools;
import com.radio.codec2talkie.tools.DebugTools;
import com.radio.codec2talkie.transport.SoundModem;
import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Hdlc implements Protocol {
    private static final String TAG = Hdlc.class.getSimpleName();

    private static final int RX_BUFFER_SIZE = 8192;

    protected Transport _transport;
    private ProtocolCallback _parentProtocolCallback;

    protected final byte[] _rxDataBuffer;
    protected final ByteBuffer _currentFrameBuffer;

    private final int _prefixSymCount;

    private int _readByte = 0;
    private int _prevHdlc = 0;

    public Hdlc(SharedPreferences sharedPreferences) {
        double preambleLenSec = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.PORTS_SOUND_MODEM_PREAMBLE, "200")) / 1000.0;
        String modemType = sharedPreferences.getString(PreferenceKeys.PORTS_SOUND_MODEM_TYPE, "1200");
        // FIXME if more modulation schemes
        int modemSpeed = modemType.equals("300") ? 300 : 1200;
        _prefixSymCount = (int) (preambleLenSec * modemSpeed / 8);

        _rxDataBuffer = new byte[RX_BUFFER_SIZE];
        _currentFrameBuffer = ByteBuffer.allocate(RX_BUFFER_SIZE);
    }

    @Override
    public void initialize(Transport transport, Context context, ProtocolCallback protocolCallback) throws IOException {
        _transport = transport;
        _parentProtocolCallback = protocolCallback;
    }

    @Override
    public int getPcmAudioBufferSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendPcmAudio(String src, String dst, int codec, short[] pcmFrame) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendCompressedAudio(String src, String dst, int codec, byte[] frame) throws IOException {
        _transport.write(hdlcEncode(frame));
    }

    @Override
    public void sendTextMessage(TextMessage textMessage) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendData(String src, String dst, byte[] dataPacket) throws IOException {
        _transport.write(hdlcEncode(dataPacket));
    }

    @Override
    public boolean receive() throws IOException {
        int bitsRead = _transport.read(_rxDataBuffer);
        if (bitsRead > 0) {
            byte[] data = Arrays.copyOf(_rxDataBuffer, bitsRead);
            for (byte bit : data) {
                _readByte <<= 1;
                _readByte |= bit;
                _readByte &= 0xff;
                if (_readByte == 0x7e) {
                    //Log.i(TAG, "HDLC " + _prevHdlc/8);
                    int pos = _currentFrameBuffer.position();

                    // shift/flush back previous 8 - 1 bits
                    if (pos >= 7) {
                        _currentFrameBuffer.position(_currentFrameBuffer.position() - 7);
                    } else {
                        _currentFrameBuffer.position(0);
                    }
                    // get packet bits between 0x7e
                    _currentFrameBuffer.flip();
                    byte[] packetBits = new byte[_currentFrameBuffer.remaining()];
                    _currentFrameBuffer.get(packetBits);

                    // get bytes from bits
                    byte[] packetBytes = BitTools.convertFromHDLCBitArray(packetBits);
                    if (packetBytes != null) {
                        //Log.i(TAG, DebugTools.byteBitsToString(packetBits));
                        //Log.i(TAG, DebugTools.bytesToHex(packetBytes));
                        if (packetBytes.length > 2) {
                            byte[] contentBytes = Arrays.copyOf(packetBytes, packetBytes.length - 2);
                            int calculatedCrc = ChecksumTools.calculateFcs(contentBytes);
                            int packetCrc = ((int)packetBytes[packetBytes.length - 2] & 0xff) | (((int)packetBytes[packetBytes.length - 1] & 0xff) << 8);
                            //Log.i(TAG, "checksum: " + calculatedCrc + " " + packetCrc);
                            if (calculatedCrc == packetCrc) {
                                //Log.v(TAG, DebugTools.byteBitsToString(packetBits));
                                Log.i(TAG, "RX: " + DebugTools.bytesToHex(packetBytes));
                                _parentProtocolCallback.onReceiveCompressedAudio(null, null, -1, contentBytes);
                            }
                        }
                    }
                    _currentFrameBuffer.clear();
                    _readByte = 0;
                    _prevHdlc = 0;
                } else {
                    try {
                        _currentFrameBuffer.put(bit);
                    } catch (BufferOverflowException e) {
                        e.printStackTrace();
                        _currentFrameBuffer.clear();
                    }
                    _prevHdlc++;
                }
            }
        }
        return false;
    }

    @Override
    public void sendPosition(Position position) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

    public byte[] genPreamble(int count) {
        byte[] preamble = new byte[count];
        for (int i = 0; i < count; i++)
            preamble[i] = (byte)0x7e;
        return BitTools.convertToHDLCBitArray(preamble, false);
    }

    public byte[] hdlcEncode(byte[] dataSrc) {
        ByteBuffer buffer = ByteBuffer.allocate(dataSrc.length + 2);

        // include checksum
        buffer.put(dataSrc);
        int fcs = ChecksumTools.calculateFcs(dataSrc);
        // least significant byte first
        buffer.put((byte)(fcs & 0xff));
        buffer.put((byte)((fcs >> 8) & 0xff));

        // convert to bits
        buffer.flip();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        //Log.i(TAG, "TX: " + DebugTools.bytesToHex(data));

        byte[] dataBytesAsBits = BitTools.convertToHDLCBitArray(data, true);

        // add preamble
        ByteBuffer hdlcBitBuffer = ByteBuffer.allocate(dataBytesAsBits.length + 8*_prefixSymCount + 8*10);
        hdlcBitBuffer.put(genPreamble(_prefixSymCount));
        hdlcBitBuffer.put(dataBytesAsBits);
        hdlcBitBuffer.put(genPreamble(10));

        // return
        hdlcBitBuffer.flip();
        byte[] r = new byte[hdlcBitBuffer.remaining()];
        hdlcBitBuffer.get(r);
        return r;
    }
}
