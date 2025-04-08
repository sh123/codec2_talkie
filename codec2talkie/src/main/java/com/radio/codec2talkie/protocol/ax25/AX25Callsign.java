package com.radio.codec2talkie.protocol.ax25;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

public class AX25Callsign {
    public static int CallsignMaxSize = 7;

    public String callsign;
    public int ssid;
    public boolean isValid;
    public boolean isLast = false;

    public AX25Callsign() {}

    public AX25Callsign(String callsign, String ssid) {
        this.callsign = callsign;
        this.ssid = Integer.parseInt(ssid);
    }

    public static String formatCallsign(String callsign, String ssid) {
        return String.format("%s-%s", callsign, ssid);
    }

    public void fromString(String inputCallsignWithSsid) {
        isValid = false;
        if (inputCallsignWithSsid == null) return;
        // WIDE1*
        String callsignWithSsid = inputCallsignWithSsid.replace("*", "");
        // ABCDEF-XX
        if (callsignWithSsid.length() > CallsignMaxSize + 2 || callsignWithSsid.isEmpty()) return;
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
        if (data == null) return;
        if (data.length != CallsignMaxSize) return;

        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < data.length - 1; i++) {
            int d = (data[i] & 0xff) >>> 1;
            char c = (char)d;
            if (c == ' '){
                break;
            } else {
                buffer.append(c);
            }
        }
        callsign = buffer.toString();
        byte lastByte = data[data.length - 1];
        isLast = (lastByte & 0x01) == 1;
        ssid = (lastByte >>> 1) & 0x0f;

        if (callsign.length() == 0) return;
        isValid = true;
    }

    @NonNull
    public String toString() {
        String callsignPlusSsid = callsign;
        if (ssid == 0) {
             if (isWide())
                callsignPlusSsid += "*";
        } else {
            callsignPlusSsid += "-" + ssid;
        }
        return callsignPlusSsid;
    }

    public byte[] toBinary() {
        ByteBuffer buffer = ByteBuffer.allocate(CallsignMaxSize);
        for (int i = 0; i < CallsignMaxSize - 1; i++) {
            if (i < callsign.length()) {
                byte c = (byte) callsign.charAt(i);
                buffer.put((byte)(c << 1));
            } else {
                // append ' ' for short callsigns
                buffer.put((byte)(0x20 << 1));
            }
        }
        byte binSsid = (byte)(ssid << 1);
        if (isLast) {
            binSsid |= 1;
        }
        buffer.put(binSsid);
        // return
        buffer.flip();
        byte[] b = new byte[buffer.remaining()];
        buffer.get(b);
        return b;
    }

    public boolean isWide() {
        return callsign.toUpperCase().startsWith("WIDE");
    }

    public boolean isTrace() {
        return callsign.toUpperCase().startsWith("TRACE");
    }

    public boolean isSoftware() {
        return callsign.toUpperCase().matches("^(AP)[A-Z]{1,4}$");
    }

    public boolean isPath() {
        return isWide();
    }

    public boolean digiRepeatCallsign() {
        if (ssid > 0) {
            ssid -= 1;
            return true;
        }
        return false;
    }

    public boolean digiRepeat() {
        if (isPath()) {
            return digiRepeatCallsign();
        }
        return false;
    }
}
