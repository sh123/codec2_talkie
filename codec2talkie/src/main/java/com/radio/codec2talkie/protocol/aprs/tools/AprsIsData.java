package com.radio.codec2talkie.protocol.aprs.tools;

public class AprsIsData {
    public String src;
    public String dst;
    public String path;
    public String data;

    public static AprsIsData fromString(String textData) {
        AprsIsData aprsIsData = new AprsIsData();
        String[] callsignData = textData.split(">");
        if (callsignData.length < 2) return null;
        aprsIsData.src = callsignData[0];
        String[] digipathData = callsignData[1].split(":");
        if (digipathData.length < 2) return null;
        String[] path = digipathData[0].split(",");
        if (path.length == 0) return null;
        aprsIsData.dst = path[0];
        aprsIsData.data = digipathData[1];
        String[] filteredPath = new String[path.length - 1];
        System.arraycopy(path, 1, filteredPath, 0, path.length - 1);
        StringBuilder digipath = new StringBuilder();
        String sep = "";
        for (String p : filteredPath) {
            if (p.startsWith("WIDE")) {
                digipath.append(sep);
                digipath.append(p);
                sep = ",";
            }
        }
        aprsIsData.path = digipath.toString();
        return aprsIsData;
    }
}
