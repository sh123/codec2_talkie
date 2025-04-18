package com.radio.codec2talkie.protocol.aprs.tools;

import androidx.annotation.NonNull;

import kotlin.text.Regex;
import kotlin.text.RegexOption;

public class AprsIsData {
    public String src;
    public String dst;
    public String digipath;
    public String rawDigipath;
    public String data;
    public AprsIsData thirdParty;

    public AprsIsData() {
    }

    public AprsIsData(String src, String dst, String path, String data) {
        this.src = src;
        this.dst = dst;
        this.digipath = path;
        this.data = data;
        // handle third party packet
        if (data.length() > 10 && data.startsWith("}")) {
            thirdParty = AprsIsData.fromString(data.substring(1));
        }
    }

    public boolean hasThirdParty() {
        return thirdParty != null;
    }

    @NonNull
    public String convertToString(boolean useRawPath) {
        String result = src + ">";
        if (dst != null && !dst.isEmpty())
            result += dst;
        if (useRawPath && rawDigipath != null && !rawDigipath.isEmpty())
            result += "," + rawDigipath;
        else if (digipath != null && !digipath.isEmpty())
            result += "," + digipath;
        result += ":" + data;
        return result;
    }

    public boolean isEligibleForRxGate() {
        boolean hasNoGate = rawDigipath.contains("TCPIP") ||
                rawDigipath.contains("TCPXX") ||
                rawDigipath.contains("NOGATE") ||
                rawDigipath.contains("RFONLY");

        boolean thirdPartyHasNoGate = thirdParty != null &&
                (thirdParty.rawDigipath.contains("TCPIP") ||
                        thirdParty.rawDigipath.contains("TCPXX"));

        // do not gate TCPIP/NOGATE, queries and third party tcp ip packets
        return !hasNoGate && !data.startsWith("?") && !thirdPartyHasNoGate;
    }

    public boolean isEligibleForTxGate() {
        return !(rawDigipath.contains("TCPXX") ||
                rawDigipath.contains("NOGATE") ||
                rawDigipath.contains("RFONLY"));
    }

    public static AprsIsData fromString(String textData) {
        AprsIsData aprsIsData = new AprsIsData();
        // N0CALL>PATH:DATA
        String[] callsignData = textData.split(">");
        if (callsignData.length < 2) return null;
        aprsIsData.src = callsignData[0];
        // PATH:DATA
        String[] digipathData = joinTail(callsignData, ">", ".*").split(":");
        if (digipathData.length < 2) return null;
        // DST,PATH1,PATH2,...
        String[] path = digipathData[0].split(",");
        if (path.length == 0) return null;
        aprsIsData.dst = path[0];
        aprsIsData.digipath = joinTail(path, ",", "^WIDE.+$");
        aprsIsData.rawDigipath = joinTail(path, ",", ".*");
        aprsIsData.data = joinTail(digipathData, ":", ".*");
        if (aprsIsData.data.length() > 10 && aprsIsData.data.startsWith("}")) {
            aprsIsData.thirdParty = AprsIsData.fromString(aprsIsData.data.substring(1));
        }
        return aprsIsData;
    }

    private static String joinTail(String[] data, String separator, String filterRegex) {
        StringBuilder result = new StringBuilder();
        if (data.length < 2) return result.toString();
        String[] tail = new String[data.length - 1];
        System.arraycopy(data, 1, tail, 0, data.length - 1);
        String sep = "";
        Regex regex = new Regex(filterRegex, RegexOption.DOT_MATCHES_ALL);
        for (String p : tail) {
            if (regex.matches(p)) {
                result.append(sep);
                result.append(p);
                sep = separator;
            }
        }
        return result.toString();
    }
}
