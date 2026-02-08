package com.radio.codec2talkie.tools;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

public class TextTools {
    public static String addZeroWidthSpaces(String text) {
        return text.replaceAll(".(?!$)", "$0\u200b");
    }

    public static int countChars(String text, char ch) {
        int count = 0;

        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == ch) {
                count++;
            }
        }
        return count;
    }

    public static String stripNulls(String text) {
        int pos = text.indexOf('\0');
        if (pos == -1) return text;
        return text.substring(0, pos);
    }

    public static byte[] stripNulls(byte[] data) {
        int i = 0;
        for (byte b : data) {
            if (b == 0) break;
            i++;
        }
        if (i == data.length) return data;
        return Arrays.copyOf(data, i);
    }

    public static String getString(ByteBuffer byteBuffer) {
        StringBuilder result = new StringBuilder();
        if (byteBuffer.position() > 0) {
            byteBuffer.flip();
            Charset charset = StandardCharsets.ISO_8859_1;
            while (byteBuffer.hasRemaining()) {
                byte b = byteBuffer.get();
                char c = charset.decode(ByteBuffer.wrap(new byte[]{b})).toString().charAt(0);
                if (c == '\n') {
                    break;
                }
                result.append(c);
            }
            byteBuffer.compact();
        }
        return result.toString().replaceAll("[\\r\\n]+$", "");
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
            "abcdefghijklmnopqrstuvwxyz" +
            "0123456789";

    // Method to generate a random string of specified length
    public static String generateRandomString(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder randomString = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(CHARACTERS.length());
            randomString.append(CHARACTERS.charAt(index));
        }

        return randomString.toString();
    }
}
