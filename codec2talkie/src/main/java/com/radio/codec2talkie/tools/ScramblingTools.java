package com.radio.codec2talkie.tools;

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

public class ScramblingTools {

    public static final int SALT_BYTES = 8;
    public static final int BLOCK_SIZE = 16;

    private static final String SCRAMBLING_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String PBE_ALGORITHM = "PBEwithSHA256and128BITAES-CBC-BC";

    private static final SecureRandom _randomGenerator = new SecureRandom();

    public static ScrambledData scramble(String masterKey, byte[] rawData, int iterations)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {

        ScrambledData encData = new ScrambledData();
        encData.salt = new byte[SALT_BYTES];
        encData.iv = new byte[BLOCK_SIZE];

        _randomGenerator.nextBytes(encData.salt);
        _randomGenerator.nextBytes(encData.iv);

        PBEKeySpec keySpec = new PBEKeySpec(masterKey.toCharArray(), encData.salt, iterations);
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(PBE_ALGORITHM);

        Key key = secretKeyFactory.generateSecret(keySpec);
        Cipher cipher = Cipher.getInstance(SCRAMBLING_ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(encData.iv);

        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

        encData.scrambledData = cipher.doFinal(rawData);

        return encData;
    }

    public static byte[] unscramble(String masterKey, ScrambledData scrambledData, int iterations)
            throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {

        PBEKeySpec keySpec = new PBEKeySpec(masterKey.toCharArray(), scrambledData.salt, iterations);
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(PBE_ALGORITHM);

        Key key = secretKeyFactory.generateSecret(keySpec);

        Cipher cipher = Cipher.getInstance(SCRAMBLING_ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(scrambledData.iv);

        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

        return cipher.doFinal(scrambledData.scrambledData);
    }

    public static class ScrambledData {
        public byte[] salt;
        public byte[] iv;
        public byte[] scrambledData;
    }
}
