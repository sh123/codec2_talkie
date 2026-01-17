package com.radio.codec2talkie.protocol.ciphers;

import android.util.Log;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;

public class Aes256Hmac implements ProtocolCipher {
    private static final String TAG = Aes128Simple.class.getSimpleName();

    public static final int SALT_SIZE_BYTES = 16;
    public static final int IV_SIZE_BYTES = 16;
    private static final int HMAC_SIZE_BYTES = 32;
    public static final int ITERATIONS_COUNT = 10000;

    private static final String CRYPTO_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String PBE_ALGORITHM = "PBEwithSHA256and256BITAES-CBC-BC";
    private static final String HMAC_SHA256 = "HmacSHA256";

    private static final SecureRandom _randomGenerator = new SecureRandom();
    private static String _masterKey;

    public void setKey(String key)  {
        _masterKey = key;
    }

    @Override
    public byte[] encrypt(byte[] rawData) {
        try {
            // derive temporary key from master key
            byte[] salt = new byte[SALT_SIZE_BYTES];
            _randomGenerator.nextBytes(salt);
            PBEKeySpec keySpec = new PBEKeySpec(_masterKey.toCharArray(), salt, ITERATIONS_COUNT);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(PBE_ALGORITHM);
            Key key = secretKeyFactory.generateSecret(keySpec);

            // generate iv and encrypt
            Cipher cipher = Cipher.getInstance(CRYPTO_ALGORITHM);
            byte[] iv = new byte[IV_SIZE_BYTES];
            _randomGenerator.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
            byte[] encryptedData = cipher.doFinal(rawData);

            // prepare encrypted packet with iv and salt included
            byte[] encryptedPacket = new byte[iv.length + salt.length + encryptedData.length];
            System.arraycopy(iv, 0, encryptedPacket, 0, iv.length);
            System.arraycopy(salt, 0, encryptedPacket, iv.length, salt.length);
            System.arraycopy(encryptedData, 0, encryptedPacket, iv.length + salt.length, encryptedData.length);

            // generate hmac key from master key
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(key);
            byte[] hmac = mac.doFinal(encryptedPacket);

            // generate result packet, combine encrypted packet with hmac
            byte[] result = new byte[encryptedPacket.length + hmac.length];
            System.arraycopy(encryptedPacket, 0, result, 0, encryptedPacket.length);
            System.arraycopy(hmac, 0, result, encryptedPacket.length, hmac.length);
            return result;

        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeySpecException |
                 InvalidKeyException | BadPaddingException | IllegalBlockSizeException |
                 InvalidAlgorithmParameterException e) {

            e.printStackTrace();
        }
        return null;
    }

    @Override
    public byte[] decrypt(byte[] encryptedPacket) {

        // check for packet validity
        int dataSize = encryptedPacket.length - IV_SIZE_BYTES - SALT_SIZE_BYTES - HMAC_SIZE_BYTES;
        if (dataSize <= 0) {
            Log.e(TAG, "Frame of wrong length " + dataSize);
            return null;
        }

        // extract data from the packet
        byte[] iv = new byte[IV_SIZE_BYTES];
        byte[] salt = new byte [SALT_SIZE_BYTES];
        byte[] encryptedData = new byte[dataSize];
        byte[] hmac = new byte [HMAC_SIZE_BYTES];
        System.arraycopy(encryptedPacket, 0, iv, 0, iv.length);
        System.arraycopy(encryptedPacket, iv.length, salt, 0, salt.length);
        System.arraycopy(encryptedPacket, iv.length + salt.length, encryptedData, 0, encryptedData.length);
        System.arraycopy(encryptedPacket, iv.length + salt.length + encryptedData.length, hmac, 0, hmac.length);

        try {
            // derive key from master key and received salt
            PBEKeySpec keySpec = new PBEKeySpec(_masterKey.toCharArray(), salt, ITERATIONS_COUNT);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(PBE_ALGORITHM);
            Key key = secretKeyFactory.generateSecret(keySpec);

            // verify hmac
            byte[] hmacVerifyData = new byte[dataSize + IV_SIZE_BYTES + SALT_SIZE_BYTES];
            System.arraycopy(encryptedPacket, 0, hmacVerifyData, 0, hmacVerifyData.length);
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(key);
            byte[] calculatedHmac = mac.doFinal(hmacVerifyData);
            if (!Arrays.equals(hmac, calculatedHmac)) {
                return null;
            }

            // decrypt
            Cipher cipher = Cipher.getInstance(CRYPTO_ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
            return cipher.doFinal(encryptedData);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 BadPaddingException | IllegalBlockSizeException |
                 InvalidAlgorithmParameterException | InvalidKeySpecException e) {

            e.printStackTrace();
            return null;
        }
    }
}
