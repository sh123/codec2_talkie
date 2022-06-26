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

public class ScramblerPipe implements Protocol {

    private static final String TAG = ScramblerPipe.class.getSimpleName();

    private final Protocol _childProtocol;
    private final String _scramblingKey;

    private int _iterationsCount;

    public ScramblerPipe(Protocol childProtocol, String scramblingKey) {
        _childProtocol = childProtocol;
        _scramblingKey = scramblingKey;
    }

    @Override
    public void initialize(Transport transport, Context context) throws IOException {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        _iterationsCount = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.KISS_SCRAMBLER_ITERATIONS, "1000"));
        _childProtocol.initialize(transport, context);
    }

    @Override
    public void sendAudio(String src, String dst, byte[] audioFrame) throws IOException {
        byte[] result = scramble(audioFrame);
        if (result != null) {
            _childProtocol.sendData(src, dst, result);
        }
    }

    @Override
    public void sendData(String src, String dst, byte[] dataPacket) throws IOException {
        byte[] result = scramble(dataPacket);
        if (result != null) {
            _childProtocol.sendData(src, dst, result);
        }
    }

    @Override
    public boolean receive(Callback callback) throws IOException {
        return _childProtocol.receive(new Callback() {
            @Override
            protected void onReceiveAudioFrames(String src, String dst, byte[] scrambledFrame) {

                byte[] audioFrames = unscramble(scrambledFrame, callback);
                if (audioFrames != null) {
                    callback.onReceiveAudioFrames(src, dst, audioFrames);
                }
            }

            @Override
            protected void onReceiveData(String src, String dst, byte[] scrambledData) {
                byte[] data = unscramble(scrambledData, callback);
                if (data != null) {
                    callback.onReceiveData(src, dst, data);
                }
            }

            @Override
            protected void onReceiveSignalLevel(byte[] rawData) {
                callback.onReceiveSignalLevel(rawData);
            }

            @Override
            protected void onProtocolRxError() {
                callback.onProtocolRxError();
            }
        });
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

    private byte[] unscramble(byte[] scrambledData, Callback callback) {
        ScramblingTools.ScrambledData data = new ScramblingTools.ScrambledData();

        data.iv = new byte[ScramblingTools.BLOCK_SIZE];
        data.salt = new byte [ScramblingTools.SALT_BYTES];
        int dataSize = scrambledData.length - ScramblingTools.BLOCK_SIZE - ScramblingTools.SALT_BYTES;
        if (dataSize <= 0) {
            Log.e(TAG, "Frame of wrong length " + dataSize);
            callback.onProtocolRxError();
            return null;
        }
        data.scrambledData = new byte[dataSize];

        System.arraycopy(scrambledData, 0, data.iv, 0, data.iv.length);
        System.arraycopy(scrambledData, data.iv.length, data.salt, 0, data.salt.length);
        System.arraycopy(scrambledData, data.iv.length + data.salt.length, data.scrambledData, 0, data.scrambledData.length);

        byte[] unscrambledData = null;
        try {
            unscrambledData = ScramblingTools.unscramble(_scramblingKey, data, _iterationsCount);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException |
                InvalidKeyException | BadPaddingException | IllegalBlockSizeException |
                InvalidAlgorithmParameterException e) {

            e.printStackTrace();
            callback.onProtocolRxError();
        }
        return unscrambledData;
    }
}
