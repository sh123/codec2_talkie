package com.radio.codec2talkie.protocol.ax25;

public class AX25Callsign {
    public String callsign;
    public String ssid;
    public boolean isValid;

    public void fromString(String callsignWithSsid) {
        String[] callsignSsid = callsignWithSsid.split("-", 2);
        if (callsignSsid.length == 2) {
            callsign = callsignSsid[0];
            ssid = callsignSsid[1];
            isValid = true;
        } else {
            isValid = false;
        }
    }

    public void fromBinary(byte[] data) {
    }

    public String toString() {
        return callsign + "-" + ssid;
    }

    public byte[] toBinary() {
        return null;
    }
}
