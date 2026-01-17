package com.radio.codec2talkie.protocol.ciphers;

public interface ProtocolCipher {
    void setKey(String key);
    byte[] encrypt(byte[] data);
    byte[] decrypt(byte[] encryptedData);
}
