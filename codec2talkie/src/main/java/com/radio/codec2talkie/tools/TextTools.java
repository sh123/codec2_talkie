package com.radio.codec2talkie.tools;

import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

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
            while (byteBuffer.hasRemaining()) {
                char c = (char)byteBuffer.get();
                if (c == '\n') {
                    break;
                }
                result.append(c);
            }
            byteBuffer.compact();
        }
        return result.toString();
    }
}
