package com.radio.codec2talkie.storage.log.group;

public class LogItemGroup {
    private String srcCallsign;

    public LogItemGroup(String srcCallsign) {
        this.srcCallsign = srcCallsign;
    }

    public String getSrcCallsign() { return srcCallsign; }

    public void setSrcCallsign(String srcCallsign) { this.srcCallsign = srcCallsign; }
}
