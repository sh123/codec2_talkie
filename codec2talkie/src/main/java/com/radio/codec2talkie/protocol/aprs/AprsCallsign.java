package com.radio.codec2talkie.protocol.aprs;

import com.radio.codec2talkie.protocol.ax25.AX25Callsign;

public class AprsCallsign {
    public boolean isValid;

    private final AX25Callsign _ax25Callsign;

    public AprsCallsign(String textCallsign) {
        _ax25Callsign = new AX25Callsign();
        _ax25Callsign.fromString(textCallsign);
        isValid = _ax25Callsign.isValid;
    }

    public boolean isSoftware() {
        if (isValid) {
            return _ax25Callsign.callsign.matches("^(AP|ap)[A-Za-z0-9]{1,4}$");
        }
        return false;
    }
}
