package com.radio.codec2talkie.tools;

public class TextTools {
    public static String addZeroWidthSpaces(String text) {
        return text.replaceAll(".(?!$)", "$0\u200b");
    }
}
