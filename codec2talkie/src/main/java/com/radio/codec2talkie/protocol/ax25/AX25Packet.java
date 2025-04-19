package com.radio.codec2talkie.protocol.ax25;

import androidx.annotation.NonNull;

import com.radio.codec2talkie.protocol.aprs.tools.AprsIsData;
import com.radio.codec2talkie.tools.DebugTools;
import com.radio.codec2talkie.tools.TextTools;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class AX25Packet {

    public static int MaximumSize = 512;
    public static int MaximumRptCount = 7;

    public String src;
    public String dst;
    public String digipath;
    public boolean isAudio;
    public byte[] rawData;
    public boolean isValid;

    private final byte AX25CTRL_UI = (byte)0x03;
    private final byte AX25PID_NO_LAYER3 = (byte)0xf0;
    private final byte AX25PID_AUDIO = (byte)0xf1;

    public void fromBinary(byte[] data) {
        isValid = false;
        if (data == null) return;
        // lora text packet with 0x3c,0xff,0x01 prefix
        if (data.length > 3 && data[0] == (byte)0x3c && data[1] == (byte)0xff && data[2] == (byte)0x01) {
            String rawText = new String(Arrays.copyOfRange(data, 3, data.length), StandardCharsets.ISO_8859_1);
            AprsIsData textPacket = AprsIsData.fromString(rawText);
            if (textPacket != null) {
                src = textPacket.src;
                dst = textPacket.dst;
                digipath = textPacket.rawDigipath;
                rawData = textPacket.data.getBytes(StandardCharsets.ISO_8859_1);
                isAudio = false;
                isValid = true;
                return;
            }
        }
        // binary packet
        ByteBuffer buffer = ByteBuffer.wrap(data);
        try {
            // dst
            AX25Callsign dstCallsign = new AX25Callsign();
            byte[] dstBytes = new byte[AX25Callsign.CallsignMaxSize];
            buffer.get(dstBytes);
            dstCallsign.fromBinary(dstBytes);
            if (!dstCallsign.isValid) return;
            dst = dstCallsign.toString();
            // src
            AX25Callsign srcCallsign = new AX25Callsign();
            byte[] srcBytes = new byte[AX25Callsign.CallsignMaxSize];
            buffer.get(srcBytes);
            srcCallsign.fromBinary(srcBytes);
            if (!srcCallsign.isValid) return;
            src = srcCallsign.toString();
            // digipath
            if (!srcCallsign.isLast) {
                StringBuilder rptBuilder = new StringBuilder();
                for (int i = 0; i < MaximumRptCount; i++) {
                    AX25Callsign rptCallsign = new AX25Callsign();
                    byte[] rptBytes = new byte[AX25Callsign.CallsignMaxSize];
                    buffer.get(rptBytes);
                    rptCallsign.fromBinary(rptBytes);
                    if (!rptCallsign.isValid) return;
                    rptBuilder.append(rptCallsign);
                    if (rptCallsign.isLast) break;
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
        } catch (BufferUnderflowException e) {
            //e.printStackTrace();
        }
    }

    public byte[] toTextBinary() {
        byte[] packetContent = toString().getBytes(StandardCharsets.ISO_8859_1);
        // lora aprs prefix 0x3c,0xff,0x01
        ByteBuffer textPacketBuffer = ByteBuffer.allocateDirect(packetContent.length + 3);
        textPacketBuffer.put((byte)0x3c).put((byte)0xff).put((byte)0x01);
        textPacketBuffer.put(packetContent);
        textPacketBuffer.flip();
        byte[] ax25Frame = new byte[textPacketBuffer.remaining()];
        textPacketBuffer.get(ax25Frame);
        return ax25Frame;
    }

    public byte[] toBinary() {
        ByteBuffer buffer = ByteBuffer.allocate(MaximumSize);
        String[] rptCallsigns = new String[] {};
        if (digipath != null)
            rptCallsigns = digipath.replaceAll(" ", "").split(",");
        boolean hasRtpCallsigns = rptCallsigns.length > 0 &&
                !(rptCallsigns.length == 1 && rptCallsigns[0].isEmpty());
        // dst
        AX25Callsign dstCallsign = new AX25Callsign();
        dstCallsign.fromString(dst);
        if (!dstCallsign.isValid) return null;
        buffer.put(dstCallsign.toBinary());
        // src
        AX25Callsign srcCallsign = new AX25Callsign();
        srcCallsign.fromString(src);
        if (!srcCallsign.isValid) return null;
        if (!hasRtpCallsigns) srcCallsign.isLast = true;
        buffer.put(srcCallsign.toBinary());
        // digipath
        for (int i = 0; i < rptCallsigns.length; i++) {
            String callsign = rptCallsigns[i];
            if (callsign.isEmpty()) continue;
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

    public boolean digiRepeat() {
        boolean isDigiRepeated = false;
        if (!isValid) return false;
        if (digipath == null) return digiRepeatMicE();
        String[] digiPaths = digipath.split(",");
        if (digiPaths.length == 0) return digiRepeatMicE();
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < digiPaths.length; i++) {
            AX25Callsign rptCallsign = new AX25Callsign();
            rptCallsign.fromString(digiPaths[i]);
            if (rptCallsign.isValid) {
                if (!isDigiRepeated && rptCallsign.digiRepeat()) {
                    isDigiRepeated = true;
                    buf.append(rptCallsign);
                } else {
                    buf.append(digiPaths[i]);
                }
            } else {
                buf.append(digiPaths[i]);
            }
            if (i < digiPaths.length - 1) buf.append(",");
        }
        digipath = buf.toString();
        return isDigiRepeated;
    }

    @NonNull
    public String toString() {
        String path = digipath == null ? "" : digipath;
        if (!path.isEmpty())
            path = "," + path;
        String info = DebugTools.bytesToDebugString(rawData);
        if (!isAudio) {
            info = DebugTools.bytesToDebugString(TextTools.stripNulls(rawData));
            //info = new String(TextTools.stripNulls(rawData), StandardCharsets.ISO_8859_1);
        }
        return String.format("%s>%s%s:%s", src, dst, path, info);
    }

    private boolean digiRepeatMicE() {
        if (!isMicE()) return false;
        AX25Callsign dstRptCallsign = new AX25Callsign();
        dstRptCallsign.fromString(dst);
        if (dstRptCallsign.isValid) {
            boolean isSuccess = dstRptCallsign.digiRepeatCallsign();
            if (isSuccess) {
                dst = dstRptCallsign.toString();
                return true;
            }
        }
        return false;
    }

    private boolean isMicE() {
        return !isAudio &&
                rawData != null &&
                rawData.length > 0 &&
                rawData[0] == (byte)'`';
    }
}
