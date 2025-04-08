package com.radio.codec2talkie.protocol.aprs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AprsItem extends AprsDataPositionReport {
    private static final String TAG = AprsItem.class.getSimpleName();

    @Override
    public void fromBinary(String srcCallsign, String dstCallsign, String digipath, byte[] infoData) {
        String info = new String(infoData);

        Pattern itemPattern = Pattern.compile("^([^_!]{3,9})(!)(.+)$", Pattern.DOTALL);
        Matcher itemMatcher = itemPattern.matcher(info);
        if (!itemMatcher.matches()) return;

        String posSrcCallsign = itemMatcher.group(1);
        if (posSrcCallsign == null) return;
        String itemState = itemMatcher.group(2);
        if (itemState == null) return;
        String positionInfoData = itemMatcher.group(3);
        if (positionInfoData == null) return;

        super.fromBinary(posSrcCallsign, dstCallsign, digipath, positionInfoData.getBytes());
    }
}
