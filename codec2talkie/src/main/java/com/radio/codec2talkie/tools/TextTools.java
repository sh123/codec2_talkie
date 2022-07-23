package com.radio.codec2talkie.tools;

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
}
