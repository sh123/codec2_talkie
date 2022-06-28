package com.radio.codec2talkie.protocol.ax25;

import java.nio.ByteBuffer;

public class AX25Packet {

    public static int MaximumSize = 512;
    public static int MaximumRptCount = 7;

    public String src;
    public String dst;
    public String digipath;
    public int codec2Mode;
    public boolean isAudio;
    public byte[] rawData;
    public boolean isValid;

    private final byte AX25CTRL_UI = (byte)0x03;
    private final byte AX25PID_NO_LAYER3 = (byte)0xf0;
    private final byte AX25PID_AUDIO = (byte)0xf1;

    public void fromBinary(byte[] data) {
        isValid = false;
        ByteBuffer buffer = ByteBuffer.wrap(data);
        // src
        AX25Callsign srcCallsign = new AX25Callsign();
        byte[] srcBytes = new byte[AX25Callsign.CallsignMaxSize];
        buffer.get(srcBytes);
        srcCallsign.fromBinary(srcBytes);
        if (!srcCallsign.isValid) return;
        src = srcCallsign.toString();
        // dst
        AX25Callsign dstCallsign = new AX25Callsign();
        byte[] dstBytes = new byte[AX25Callsign.CallsignMaxSize];
        buffer.get(dstBytes);
        dstCallsign.fromBinary(dstBytes);
        if (!dstCallsign.isValid) return;
        dst = dstCallsign.toString();
        // digipath
        if (!dstCallsign.isLast) {
            StringBuilder rptBuilder = new StringBuilder();
            for (int i = 0; i < MaximumRptCount; i++) {
                AX25Callsign rptCallsign = new AX25Callsign();
                byte[] rptBytes = new byte[AX25Callsign.CallsignMaxSize];
                buffer.get(rptBytes);
                rptCallsign.fromBinary(rptBytes);
                if (!rptCallsign.isValid) return;
                rptBuilder.append(dstCallsign.toString());
                if (dstCallsign.isLast) break;
                rptBuilder.append(',');
            }
            digipath = rptBuilder.toString();
        }
        // ctrl, UI
        byte ax25Ctrl = buffer.get();
        if (ax25Ctrl != AX25CTRL_UI) return;
        // pid, isAudio
        byte ax25Pid = buffer.get();
        if (ax25Pid == AX25PID_AUDIO) {
            isAudio = true;
        } else if (ax25Pid == AX25PID_NO_LAYER3) {
            isAudio = false;
        } else {
            return;
        }
        // rawData
        rawData = new byte[buffer.remaining()];
        buffer.get(rawData);
        isValid = true;
    }

    public byte[] toBinary() {
        ByteBuffer buffer = ByteBuffer.allocate(MaximumSize);
        String[] rptCallsigns = digipath.replaceAll(" ", "").split(",");
        // src
        AX25Callsign srcCallsign = new AX25Callsign();
        srcCallsign.fromString(src);
        if (!srcCallsign.isValid) return null;
        buffer.put(srcCallsign.toBinary());
        // dst
        AX25Callsign dstCallsign = new AX25Callsign();
        dstCallsign.fromString(dst);
        if (!dstCallsign.isValid) return null;
        if (rptCallsigns.length == 0) dstCallsign.isLast = true;
        buffer.put(dstCallsign.toBinary());
        // digipath
        for (int i = 0; i < rptCallsigns.length; i++) {
            String callsign = rptCallsigns[i];
            AX25Callsign digiCallsign = new AX25Callsign();
            digiCallsign.fromString(callsign);
            if (!digiCallsign.isValid) return null;
            digiCallsign.isLast = (i == rptCallsigns.length - 1);
            buffer.put(digiCallsign.toBinary());
        }
        // flags
        buffer.put(AX25CTRL_UI);
        if (isAudio) {
            buffer.put(AX25PID_AUDIO);
        } else {
            buffer.put(AX25PID_NO_LAYER3);
        }
        // data
        buffer.put(rawData);
        // return
        buffer.flip();
        byte[] b = new byte[buffer.remaining()];
        buffer.get(b);
        return b;
    }
}
