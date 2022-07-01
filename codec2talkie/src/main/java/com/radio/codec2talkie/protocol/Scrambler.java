package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.tools.ScramblingTools;
import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Scrambler implements Protocol {

    private static final String TAG = Scrambler.class.getSimpleName();

    private final Protocol _childProtocol;
    private final String _scramblingKey;

    private int _iterationsCount;
    private Callback _parentCallback;

    public Scrambler(Protocol childProtocol, String scramblingKey) {
        _childProtocol = childProtocol;
        _scramblingKey = scramblingKey;
    }

    @Override
    public void initialize(Transport transport, Context context, Callback callback) throws IOException {
        _parentCallback = callback;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        _iterationsCount = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.KISS_SCRAMBLER_ITERATIONS, "1000"));
        _childProtocol.initialize(transport, context, _protocolCallback);
    }

    @Override
    public int getPcmAudioBufferSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendCompressedAudio(String src, String dst, int codec2Mode, byte[] audioFrame) throws IOException {
        byte[] result = scramble(audioFrame);
        if (result == null) {
            _parentCallback.onProtocolTxError();
        } else {
            _childProtocol.sendData(src, dst, result);
        }
    }

    @Override
    public void sendPcmAudio(String src, String dst, int codec, short[] pcmFrame) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendData(String src, String dst, byte[] dataPacket) throws IOException {
        byte[] result = scramble(dataPacket);
        if (result == null) {
            _parentCallback.onProtocolTxError();
        } else {
            _childProtocol.sendData(src, dst, result);
        }
    }

    @Override
    public boolean receive() throws IOException {
        return _childProtocol.receive();
    }

    Callback _protocolCallback = new Callback() {
        @Override
        protected void onReceivePosition(String src, double latitude, double longitude, double altitude, float bearing, String comment) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void onReceivePcmAudio(String src, String dst, int codec, short[] pcmFrame) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void onReceiveCompressedAudio(String src, String dst, int codec2Mode, byte[] scrambledFrame) {

            byte[] audioFrames = unscramble(scrambledFrame);
            if (audioFrames == null) {
                _parentCallback.onProtocolRxError();
            } else {
                _parentCallback.onReceiveCompressedAudio(src, dst, codec2Mode, audioFrames);
            }
        }

        @Override
        protected void onReceiveData(String src, String dst, byte[] scrambledData) {
            byte[] data = unscramble(scrambledData);
            if (data == null) {
                _parentCallback.onProtocolRxError();
            } else {
                _parentCallback.onReceiveData(src, dst, data);
            }
        }

        @Override
        protected void onReceiveSignalLevel(short rssi, short snr) {
            _parentCallback.onReceiveSignalLevel(rssi, snr);
        }

        @Override
        protected void onReceiveLog(String logData) {
            _parentCallback.onReceiveLog(logData);
        }

        @Override
        protected void onTransmitPcmAudio(String src, String dst, int codec, short[] frame) {
            _parentCallback.onTransmitPcmAudio(src, dst, codec, frame);
        }

        @Override
        protected void onTransmitCompressedAudio(String src, String dst, int codec, byte[] frame) {
            _parentCallback.onTransmitCompressedAudio(src, dst, codec, frame);
        }

        @Override
        protected void onTransmitData(String src, String dst, byte[] data) {
            _parentCallback.onTransmitData(src, dst, data);
        }

        @Override
        protected void onTransmitLog(String logData) {
            _parentCallback.onTransmitLog(logData);
        }

        @Override
        protected void onProtocolRxError() {
            _parentCallback.onProtocolRxError();
        }

        @Override
        protected void onProtocolTxError() {
            _parentCallback.onProtocolTxError();
        }
    };

    @Override
    public void sendPosition(double latitude, double longitude, double altitude, float bearing, String comment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flush() throws IOException {
        _childProtocol.flush();
    }

    @Override
    public void close() {
        _childProtocol.close();
    }

    private byte[] scramble(byte[] srcData) {
        ScramblingTools.ScrambledData data = null;
        try {
            data = ScramblingTools.scramble(_scramblingKey, srcData, _iterationsCount);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeySpecException |
                InvalidKeyException | BadPaddingException | IllegalBlockSizeException |
                InvalidAlgorithmParameterException e) {

            e.printStackTrace();
        }
        if (data != null) {
            byte[] result = new byte[data.iv.length + data.salt.length + data.scrambledData.length];

            System.arraycopy(data.iv, 0, result, 0, data.iv.length);
            System.arraycopy(data.salt, 0, result, data.iv.length, data.salt.length);
            System.arraycopy(data.scrambledData, 0, result, data.iv.length + data.salt.length, data.scrambledData.length);

            return result;
        }
        return null;
    }

    private byte[] unscramble(byte[] scrambledData) {
        ScramblingTools.ScrambledData data = new ScramblingTools.ScrambledData();

        data.iv = new byte[ScramblingTools.BLOCK_SIZE];
        data.salt = new byte [ScramblingTools.SALT_BYTES];
        int dataSize = scrambledData.length - ScramblingTools.BLOCK_SIZE - ScramblingTools.SALT_BYTES;
        if (dataSize <= 0) {
            Log.e(TAG, "Frame of wrong length " + dataSize);
            return null;
        }
        data.scrambledData = new byte[dataSize];

        System.arraycopy(scrambledData, 0, data.iv, 0, data.iv.length);
        System.arraycopy(scrambledData, data.iv.length, data.salt, 0, data.salt.length);
        System.arraycopy(scrambledData, data.iv.length + data.salt.length, data.scrambledData, 0, data.scrambledData.length);

        byte[] unscrambledData;
        try {
            unscrambledData = ScramblingTools.unscramble(_scramblingKey, data, _iterationsCount);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException |
                InvalidKeyException | BadPaddingException | IllegalBlockSizeException |
                InvalidAlgorithmParameterException e) {

            e.printStackTrace();
            return null;
        }
        return unscrambledData;
    }
}
