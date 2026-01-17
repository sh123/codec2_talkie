package com.radio.codec2talkie.protocol.ciphers;

public class Disabled implements ProtocolCipher{
    @Override
    public void setKey(String key) {
    }

    @Override
    public byte[] encrypt(byte[] data) {
        return data;
    }

    @Override
    public byte[] decrypt(byte[] encryptedData) {
        return encryptedData;
    }
}
