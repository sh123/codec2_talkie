package com.radio.codec2talkie.tools;

public class DebugTools {

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 3];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = HEX_ARRAY[v >>> 4];
            hexChars[j * 3 + 1] = HEX_ARRAY[v & 0x0F];
            hexChars[j * 3 + 2] = ' ';
        }
        return new String(hexChars);
    }

    public static boolean isPrintableAscii(byte value)
    {
        return (value > 32 ) && (value < 127);
    }

    public static String bytesToDebugString(byte[] buffer)
    {
        StringBuilder builder = new StringBuilder();
        for (byte b : buffer)
        {
            if(isPrintableAscii(b))
            {
                builder.append((char)b);
            }
            else
            {
                builder.append(String.format("<%x>", b));
            }
        }
        return builder.toString();
    }
}
