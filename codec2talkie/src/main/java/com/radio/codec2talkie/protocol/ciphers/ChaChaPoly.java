package com.radio.codec2talkie.protocol.ciphers;

import android.util.Log;

import com.radio.codec2talkie.tools.TextTools;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ChaChaPoly implements ProtocolCipher {
    private static final String TAG = ChaChaPoly.class.getSimpleName();
    private static final String CRYPTO_ALGORITHM = "ChaCha20-Poly1305";
    private static final int TAG_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int KEY_LENGTH = 32;

    private SecretKey _key;

    public void setKey(String hexKey) {
        if (hexKey.length() != KEY_LENGTH * 2) {
            throw new IllegalArgumentException("Cipher is not initialized, key must be a 256-bit (32-byte) hex string.");
        }
        byte[] keyBytes = TextTools.hexStringToByteArray(hexKey);
        _key = new SecretKeySpec(keyBytes, "ChaCha20");
    }

    public byte[] encrypt(byte[] rawData) {
        try {
            // generate iv
            byte[] iv = new byte[IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // encrypt
            Cipher cipher = Cipher.getInstance(CRYPTO_ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, _key, gcmParameterSpec);
            byte[] encryptedData = cipher.doFinal(rawData);

            // generate result packet
            byte[] encryptedPacket = new byte[IV_LENGTH + encryptedData.length];
            System.arraycopy(iv, 0, encryptedPacket, 0, IV_LENGTH);
            System.arraycopy(encryptedData, 0, encryptedPacket, IV_LENGTH, encryptedData.length);

            return encryptedPacket;
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException |
                 BadPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {

            e.printStackTrace();
        }
        return null;
    }

    public byte[] decrypt(byte[] encryptedPacket) {
        // check for packet validity
        int dataSize = encryptedPacket.length - IV_LENGTH;
        if (dataSize <= TAG_LENGTH) {
            Log.e(TAG, "Frame of wrong length " + dataSize);
            return null;
        }
        try {
            // extract data from he packet
            byte[] iv = Arrays.copyOfRange(encryptedPacket, 0, IV_LENGTH);
            byte[] encryptedData = Arrays.copyOfRange(encryptedPacket, IV_LENGTH, encryptedPacket.length);

            // decrypt
            Cipher cipher = Cipher.getInstance(CRYPTO_ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, _key, gcmParameterSpec);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
