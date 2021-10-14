package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.MainActivity;
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

    private static final String TAG = MainActivity.class.getSimpleName();

    private Context _context;

    private final Protocol _childProtocol;
    private final String _scramblingKey;

    private int _iterationsCount;

    public ScramblerPipe(Protocol childProtocol, String scramblingKey) {
        _childProtocol = childProtocol;
        _scramblingKey = scramblingKey;
    }

    @Override
    public void initialize(Transport transport, Context context) throws IOException {
        _context = context;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_context);
        _iterationsCount = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.KISS_SCRAMBLER_ITERATIONS, "1000"));
        _childProtocol.initialize(transport, context);
    }

    @Override
    public void send(byte[] audioFrame) throws IOException {
        ScramblingTools.ScrambledData data = null;
        try {
            data = ScramblingTools.scramble(_scramblingKey, audioFrame, _iterationsCount);
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

            _childProtocol.send(result);
        }
    }

    @Override
    public boolean receive(Callback callback) throws IOException {
        return _childProtocol.receive(new Callback() {
            @Override
            protected void onReceiveAudioFrames(byte[] scrambledFrame) {

                ScramblingTools.ScrambledData data = new ScramblingTools.ScrambledData();

                data.iv = new byte[ScramblingTools.BLOCK_SIZE];
                data.salt = new byte [ScramblingTools.SALT_BYTES];
                data.scrambledData = new byte[scrambledFrame.length - ScramblingTools.BLOCK_SIZE - ScramblingTools.SALT_BYTES];

                System.arraycopy(scrambledFrame, 0, data.iv, 0, data.iv.length);
                System.arraycopy(scrambledFrame, data.iv.length, data.salt, 0, data.salt.length);
                System.arraycopy(scrambledFrame, data.iv.length + data.salt.length, data.scrambledData, 0, data.scrambledData.length);

                byte[] audioFrame = null;
                try {
                    audioFrame = ScramblingTools.unscramble(_scramblingKey, data, _iterationsCount);
                } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException |
                        InvalidKeyException | BadPaddingException | IllegalBlockSizeException |
                        InvalidAlgorithmParameterException e) {

                    e.printStackTrace();
                }
                if (audioFrame != null) {
                    callback.onReceiveAudioFrames(audioFrame);
                }
            }

            @Override
            protected void onReceiveSignalLevel(byte[] rawData) {
                callback.onReceiveSignalLevel(rawData);
            }
        });
    }

    @Override
    public void flush() throws IOException {
        _childProtocol.flush();
    }
}
