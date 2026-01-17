package com.radio.codec2talkie.protocol.ciphers;

import android.util.Log;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;

public class Aes128Simple implements ProtocolCipher {
    private static final String TAG = Aes128Simple.class.getSimpleName();

    public static final int SALT_BYTES = 8;
    public static final int BLOCK_SIZE = 16;
    public static final int ITERATIONS_COUNT = 1000;

    private static final String CRYPTO_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String PBE_ALGORITHM = "PBEwithSHA256and128BITAES-CBC-BC";

    private static final SecureRandom _randomGenerator = new SecureRandom();
    private static String _masterKey;

    public void setKey(String key) {
        _masterKey = key;
    }

    @Override
    public byte[] encrypt(byte[] rawData) {
        try {
            // derive temporary key from master key
            byte[] salt = new byte[SALT_BYTES];
            _randomGenerator.nextBytes(salt);
            PBEKeySpec keySpec = new PBEKeySpec(_masterKey.toCharArray(), salt, ITERATIONS_COUNT);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(PBE_ALGORITHM);
            Key key = secretKeyFactory.generateSecret(keySpec);

            // generate iv and encrypt
            Cipher cipher = Cipher.getInstance(CRYPTO_ALGORITHM);
            byte[] iv = new byte[BLOCK_SIZE];
            _randomGenerator.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
            byte[] encryptedData = cipher.doFinal(rawData);

            // prepare result packet
            byte[] result = new byte[iv.length + salt.length + encryptedData.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(salt, 0, result, iv.length, salt.length);
            System.arraycopy(encryptedData, 0, result, iv.length + salt.length, encryptedData.length);
            return result;

        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeySpecException |
                 InvalidKeyException | BadPaddingException | IllegalBlockSizeException |
                 InvalidAlgorithmParameterException e) {

            e.printStackTrace();
        }
        return null;
    }

    @Override
    public byte[] decrypt(byte[] encryptedData) {

        // check for packet validity
        int dataSize = encryptedData.length - BLOCK_SIZE - SALT_BYTES;
        if (dataSize <= 0) {
            Log.e(TAG, "Frame of wrong length " + dataSize);
            return null;
        }

        // extract data from the packet
        byte[] iv = new byte[BLOCK_SIZE];
        byte[] salt = new byte [SALT_BYTES];
        byte[] tmpEncryptedData = new byte[dataSize];
        System.arraycopy(encryptedData, 0, iv, 0, iv.length);
        System.arraycopy(encryptedData, iv.length, salt, 0, salt.length);
        System.arraycopy(encryptedData, iv.length + salt.length, tmpEncryptedData, 0, tmpEncryptedData.length);

        try {
            // derive key from master key and received salt
            PBEKeySpec keySpec = new PBEKeySpec(_masterKey.toCharArray(), salt, ITERATIONS_COUNT);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(PBE_ALGORITHM);
            Key key = secretKeyFactory.generateSecret(keySpec);

            // decrypt
            Cipher cipher = Cipher.getInstance(CRYPTO_ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
            return cipher.doFinal(tmpEncryptedData);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 BadPaddingException | IllegalBlockSizeException |
                 InvalidAlgorithmParameterException | InvalidKeySpecException e) {

            e.printStackTrace();
            return null;
        }
    }
}
