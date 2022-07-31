package com.radio.codec2talkie.protocol;

import android.content.Context;

import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.tools.BitTools;
import com.radio.codec2talkie.tools.ChecksumTools;
import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Hdlc implements Protocol {

    protected Transport _transport;
    private ProtocolCallback _parentProtocolCallback;

    public Hdlc() {
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

        // TODO, read from settings
        int prefixLen = 30;
        int suffixLen = 5;

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
        byte[] dataBytesAsBits = BitTools.convertToHDLCBitArray(data, true);

        // add preamble
        ByteBuffer hdlcBitBuffer = ByteBuffer.allocate(dataBytesAsBits.length + 8*prefixLen + 8*suffixLen);
        hdlcBitBuffer.put(genPreamble(prefixLen));
        hdlcBitBuffer.put(dataBytesAsBits);
        hdlcBitBuffer.put(genPreamble(suffixLen));

        // return
        hdlcBitBuffer.flip();
        byte[] r = new byte[hdlcBitBuffer.remaining()];
        hdlcBitBuffer.get(r);
        return r;
    }
}
