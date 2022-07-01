package com.radio.codec2talkie.protocol.aprs;

import com.radio.codec2talkie.protocol.ax25.AX25Callsign;

public class AprsTools {

    public static boolean isAprsSoftwareCallsign(String callsign) {
        AX25Callsign ax25Callsign = new AX25Callsign();
        ax25Callsign.fromString(callsign);
        if (ax25Callsign.isValid) {
            return ax25Callsign.callsign.matches("^(AP|ap)[A-Za-z]{1,4}$");
        }
        return false;
    }
}
