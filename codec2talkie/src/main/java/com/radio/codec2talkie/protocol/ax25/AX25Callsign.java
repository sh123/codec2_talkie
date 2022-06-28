package com.radio.codec2talkie.protocol.ax25;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

public class AX25Callsign {
    public static int CallsignMaxSize = 7;

    public String callsign;
    public int ssid;
    public boolean isValid;
    public boolean isLast = false;

    public void fromString(String callsignWithSsid) {
        isValid = false;
        // ABCDEF-XX
        if (callsignWithSsid.length() > CallsignMaxSize + 2 || callsignWithSsid.length() == 0) return;
        int delimiterIndex = callsignWithSsid.indexOf('-');
        // ABCDEF-
        if (delimiterIndex != -1 && delimiterIndex == callsignWithSsid.length() - 1) return;
        callsign = callsignWithSsid;
        ssid = 0;
        if (delimiterIndex == -1) {
            // ABCDEF
            if (callsign.length() >= CallsignMaxSize) return;
        } else {
            callsign = callsignWithSsid.substring(0, delimiterIndex);
            try {
                ssid = Integer.parseInt(callsignWithSsid.substring(delimiterIndex + 1));
            } catch(NumberFormatException e) {
                return;
            }
        }
        isValid = true;
    }

    public void fromBinary(byte[] data) {
        isValid = false;
        if (data.length != CallsignMaxSize) return;

        char[] buffer = new char[CallsignMaxSize];

        for (int i = 0; i < data.length - 1; i++) {
            char c = (char)(data[i] >> 1);
            if (c == ' '){
                buffer[i] = '\0';
            } else {
                buffer[i] = c;
            }
        }
        buffer[CallsignMaxSize - 1] = '\0';

        callsign = new String(buffer);
        byte lastByte = data[data.length - 1];
        isLast = (lastByte & 0x01) == 1;
        ssid = (lastByte >> 1) & 0x0f;

        if (callsign.length() == 0) return;
        isValid = true;
    }

    @NonNull
    public String toString() {
        return callsign + "-" + ssid;
    }

    public byte[] toBinary() {
        ByteBuffer buffer = ByteBuffer.allocate(CallsignMaxSize);
        for (int i = 0; i < CallsignMaxSize - 1; i++) {
            if (i < callsign.length()) {
                byte c = (byte) callsign.charAt(i);
                buffer.put((byte) (c << 1));
            } else {
                // append ' ' for short callsigns
                buffer.put((byte)(0x20 << 1));
            }
            byte binSsid = (byte)(ssid << 1);
            if (isLast) {
                binSsid |= 1;
            }
            buffer.put(binSsid);
        }
        // return
        buffer.flip();
        byte[] b = new byte[buffer.remaining()];
        buffer.get(b);
        return b;
    }
}