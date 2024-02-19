package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.content.SharedPreferences;

import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.tools.TextTools;
import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;
import java.nio.ByteBuffer;

public class CustomDataPrefix implements Protocol {

    private final Protocol _childProtocol;
    private ProtocolCallback _parentProtocolCallback;
    private final byte[] _bytePrefix;

    public CustomDataPrefix(Protocol childProtocol, SharedPreferences sharedPreferences) {
        _childProtocol = childProtocol;
        String prefix = sharedPreferences.getString(PreferenceKeys.CUSTOM_PREFIX, "");
        _bytePrefix = TextTools.hexStringToByteArray(prefix);
    }

    @Override
    public void initialize(Transport transport, Context context, ProtocolCallback protocolCallback) throws IOException {
        _parentProtocolCallback = protocolCallback;
        _childProtocol.initialize(transport, context, _protocolCallback);
    }

    @Override
    public int getPcmAudioRecordBufferSize() {
        return -1;
    }

    @Override
    public void sendCompressedAudio(String src, String dst, byte[] frame) throws IOException {
        byte[] prefixedData = ByteBuffer.allocate(_bytePrefix.length + frame.length)
                .put(_bytePrefix)
                .put(frame)
                .array();
        _childProtocol.sendCompressedAudio(src, dst, prefixedData);
    }

    @Override
    public void sendTextMessage(TextMessage textMessage) throws IOException {
        _childProtocol.sendTextMessage(textMessage);
    }

    @Override
    public void sendPcmAudio(String src, String dst, short[] pcmFrame) throws IOException {
        _childProtocol.sendPcmAudio(src, dst, pcmFrame);
    }

    @Override
    public void sendData(String src, String dst, String path, byte[] dataPacket) throws IOException {
        byte[] prefixedData = ByteBuffer.allocate(_bytePrefix.length + dataPacket.length)
                .put(_bytePrefix)
                .put(dataPacket)
                .array();
        _childProtocol.sendData(src, dst, path, prefixedData);
    }

    @Override
    public boolean receive() throws IOException {
        return _childProtocol.receive();
    }

    @Override
    public void sendPosition(Position position) throws IOException {
        _childProtocol.sendPosition(position);
    }

    @Override
    public void flush() throws IOException {
        _childProtocol.flush();
    }

    @Override
    public void close() {
        _childProtocol.close();
    }

    ProtocolCallback _protocolCallback = new ProtocolCallback() {
        @Override
        protected void onReceivePosition(Position position) {
            _parentProtocolCallback.onReceivePosition(position);
        }

        @Override
        protected void onReceivePcmAudio(String src, String dst, short[] pcmFrame) {
            _parentProtocolCallback.onReceivePcmAudio(src, dst, pcmFrame);
        }

        @Override
        protected void onReceiveCompressedAudio(String src, String dst, byte[] audioFrame) {
            _parentProtocolCallback.onReceiveCompressedAudio(src, dst, audioFrame);
        }

        @Override
        protected void onReceiveTextMessage(TextMessage textMessage) {
            _parentProtocolCallback.onReceiveTextMessage(textMessage);
        }

        @Override
        protected void onReceiveData(String src, String dst, String path, byte[] data) {
            _parentProtocolCallback.onReceiveData(src, dst, path, data);
        }

        @Override
        protected void onReceiveSignalLevel(short rssi, short snr) {
            _parentProtocolCallback.onReceiveSignalLevel(rssi, snr);
        }

        @Override
        protected void onReceiveTelemetry(int batVoltage) {
            _parentProtocolCallback.onReceiveTelemetry(batVoltage);
        }

        @Override
        protected void onReceiveLog(String logData) {
            _parentProtocolCallback.onReceiveLog(logData);
        }

        @Override
        protected void onTransmitPcmAudio(String src, String dst, short[] frame) {
            _parentProtocolCallback.onTransmitPcmAudio(src, dst, frame);
        }

        @Override
        protected void onTransmitCompressedAudio(String src, String dst, byte[] frame) {
            _parentProtocolCallback.onTransmitCompressedAudio(src, dst, frame);
        }

        @Override
        protected void onTransmitTextMessage(TextMessage textMessage) {
            _parentProtocolCallback.onTransmitTextMessage(textMessage);
        }

        @Override
        protected void onTransmitPosition(Position position) {
            _parentProtocolCallback.onTransmitPosition(position);
        }

        @Override
        protected void onTransmitData(String src, String dst, String path, byte[] data) {
            _parentProtocolCallback.onTransmitData(src, dst, path, data);
        }

        @Override
        protected void onTransmitLog(String logData) {
            _parentProtocolCallback.onTransmitLog(logData);
        }

        @Override
        protected void onProtocolRxError() {
            _parentProtocolCallback.onProtocolRxError();
        }

        @Override
        protected void onProtocolTxError() {
            _parentProtocolCallback.onProtocolTxError();
        }
    };
}
