package com.radio.codec2talkie.protocol.aprs;

import com.radio.codec2talkie.protocol.aprs.tools.AprsTools;
import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.tools.DeviceIdTools;
import com.radio.codec2talkie.tools.TextTools;
import com.radio.codec2talkie.tools.UnitTools;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AprsDataPositionReportMicE implements AprsData {

    private String _dstCallsign;
    private Position _position;
    private byte[] _binary;
    private boolean _isValid;

    private final Map<String, Integer> _miceMessageTypeMap = new HashMap<>() {{
        // standard
        put("off_duty", 0b111);
        put("en_route", 0b110);
        put("in_service", 0b101);
        put("returning", 0b100);
        put("committed", 0b011);
        put("special", 0b010);
        put("priority", 0b001);
        // custom
        put("custom_0", 0b111);
        put("custom_1", 0b110);
        put("custom_2", 0b101);
        put("custom_3", 0b100);
        put("custom_4", 0b011);
        put("custom_5", 0b010);
        put("custom_6", 0b001);
        // emergency
        put("emergency", 0b000);
    }};

    private final Map<Integer, String> _miceMessageReverseTypeMapStd = new HashMap<>() {{
        put(0b000, "emergency");
        put(0b111, "off_duty");
        put(0b110, "en_route");
        put(0b101, "in_service");
        put(0b100, "returning");
        put(0b011, "committed");
        put(0b010, "special");
        put(0b001, "priority");
    }};

    private final Map<Integer, String> _miceMessageReverseTypeMapCustom = new HashMap<>() {{
        put(0b000, "emergency");
        put(0b111, "custom_0");
        put(0b110, "custom_1");
        put(0b101, "custom_2");
        put(0b100, "custom_3");
        put(0b011, "custom_4");
        put(0b010, "custom_5");
        put(0b001, "custom_6");
    }};

    @Override
    public boolean isPositionReport() {
        return true;
    }

    @Override
    public boolean isTextMessage() {
        return false;
    }

    @Override
    public void fromPosition(Position position) {
        _isValid = false;
        _position = position;

        // latitude into the callsign
        _dstCallsign = generateDestinationCallsign(position);
        position.dstCallsign = _dstCallsign;

        ByteBuffer buffer = ByteBuffer.allocate(256);

        // identifier
        buffer.put((byte)'`');

        // longitude, speed/course into the information field
        buffer.put(generateInfo(position));

        // symbol code + symbol table id
        byte[] symbol = position.symbolCode.getBytes();
        buffer.put(symbol[1]);
        buffer.put(symbol[0]);

        // encode altitude if enabled
        if (position.isAltitudeEnabled && position.hasAltitude) {
            int datumAltitude = (int) (10000 + position.altitudeMeters);
            int a1 = 33 + (datumAltitude / (91 * 91));
            buffer.put((byte)a1);
            int a2 = 33 + ((datumAltitude % (91 * 91)) / 91);
            buffer.put((byte)a2);
            int a3 = 33 + ((datumAltitude % (91 * 91)) % 91);
            buffer.put((byte)a3);
            buffer.put((byte)'}');
        }

        // comment
        buffer.put(position.comment.getBytes(StandardCharsets.UTF_8));

        // return
        buffer.flip();
        _binary = new byte[buffer.remaining()];
        buffer.get(_binary);
        _isValid = true;
    }

    @Override
    public void fromTextMessage(TextMessage textMessage) {
        _isValid = false;
    }

    @Override
    public Position toPosition() {
        return _position;
    }

    @Override
    public TextMessage toTextMessage() {
        return null;
    }

    @Override
    public void fromBinary(String srcCallsign, String dstCallsign, String digipath, byte[] infoData) {
        _isValid = false;
        _position = new Position();
        _dstCallsign = dstCallsign;
        _position.srcCallsign = srcCallsign;
        _position.dstCallsign = dstCallsign;
        _position.digipath = digipath;
        _position.privacyLevel = 0;
        _position.status = "";
        _position.comment = "";

        if (srcCallsign == null || dstCallsign == null) return;
        if (dstCallsign.length() < 6 || infoData.length < 8) return;

        // read latitude
        boolean isCustom = false;
        char ns = 'S';
        char we = 'E';
        int longOffset = 0;
        int messageId = 0;
        byte[] dstCallsignBuf = dstCallsign.getBytes();
        StringBuilder latitude = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            char c = (char) dstCallsignBuf[i];
            if (c >= 'A' && c <= 'L') {
                if (i < 3) {
                    isCustom = true;
                    if (c != 'L') messageId |= 1;
                }
                if (c == 'K' || c == 'L') {
                    // NOTE, using 0 instead of ' ' for position ambiguity
                    c = '0';
                    _position.privacyLevel += 1;
                } else
                    c = (char) (c - 17);
            } else if (c >= 'P' && c <= 'Z') {
                if (i < 3) {
                    messageId |= 1;
                } else if (i == 3) {
                    ns = 'N';
                } else if (i == 4) {
                    longOffset = 100;
                } else {
                    we = 'W';
                }
                if (c == 'Z') {
                    // NOTE, using 0 instead of ' ' for position ambiguity
                    c = '0';
                    _position.privacyLevel += 1;
                } else
                    c = (char) (c - 32);
            }
            if (i < 2) messageId <<= 1;
            if (i == 4) latitude.append('.');
            latitude.append(c);
        }

        try {
            _position.latitude = UnitTools.nmeaToDecimal(latitude.toString(), Character.toString(ns));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return;
        }
        _position.status = isCustom
                ? _miceMessageReverseTypeMapCustom.get(messageId)
                : _miceMessageReverseTypeMapStd.get(messageId);

        // read longitude
        int d = ((int)infoData[0] - 28) + longOffset;
        if (d >= 180 && d <= 189) d -= 80;
        else if (d >= 190) d -= 190;

        int m = ((int)infoData[1] - 28);
        if (m >= 60) m -= 60;

        int h = ((int)infoData[2] - 28);

        String longitude = String.format(Locale.US, "%03d%02d.%02d", d, m, h);
        try {
            _position.longitude = UnitTools.nmeaToDecimal(longitude, Character.toString(we));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return;
        }

        // read course/speed
        int sp = 10 * ((int)infoData[3] - 28);
        int dcSp = ((int)infoData[4] - 28) / 10;
        int dcSe = (((int)infoData[4] - 28) % 10) * 100;
        int se = (int)infoData[5] - 28;

        int speed = sp + dcSp;
        if (speed >= 800) speed -= 800;

        int course = dcSe + se;
        if (course >= 400) course -= 400;

        _position.hasBearing = true;
        _position.bearingDegrees = course;

        _position.hasSpeed = true;
        _position.speedMetersPerSecond = UnitTools.knotsToMetersPerSecond(speed);

        // read symbol table + symbol code
        _position.symbolCode = String.format(Locale.US, "%c%c", infoData[7], infoData[6]);

        // read altitude, comment and device id
        int i = 8;
        if (infoData.length > i) {
            char c = (char)infoData[i];
            if (c == '`' || c == '\'' || c == '>' || c == ']') {
                i++;
            }
            if (infoData.length > i + 3 && (char)infoData[i + 3] == '}') {
                _position.hasAltitude = true;
                _position.altitudeMeters = ((infoData[i] - 33) * 91 * 91 + (infoData[i + 1] - 33) * 91 + (infoData[i + 2] - 33)) - 10000;
                i += 3;
            } else {
                i--;
            }
            String comment = new String(Arrays.copyOfRange(infoData, i + 1, infoData.length), StandardCharsets.UTF_8).strip();
            if (comment.length() >= 2) {
                String deviceId = comment.substring(comment.length() - 2);
                String deviceIdDescription = DeviceIdTools.getMiceDeviceDescription(deviceId);
                if (deviceIdDescription != null) {
                    _position.deviceIdDescription = deviceIdDescription;
                    comment = comment.substring(0, comment.length() - 2);
                }
            }
            _position.comment = TextTools.stripNulls(comment);
        }

        _position.maidenHead = UnitTools.decimalToMaidenhead(_position.latitude, _position.longitude);
        _isValid = true;
    }

    @Override
    public byte[] toBinary() {
        return _binary;
    }

    @Override
    public boolean isValid() {
        return _isValid;
    }

    private String generateDestinationCallsign(Position position) {

        byte[] latitude = AprsTools.applyPrivacyOnUncompressedNmeaCoordinate(
                UnitTools.decimalToNmea(position.latitude, true),
                position.privacyLevel).getBytes();
        byte[] longitude = UnitTools.decimalToNmea(position.longitude, false).getBytes();

        // get Mic-E status bit mapping
        boolean isCustom = position.status.startsWith("custom");
        Integer miceMessageTypeEncoding = _miceMessageTypeMap.get(position.status);
        miceMessageTypeEncoding = miceMessageTypeEncoding == null
                ? 0b111
                : miceMessageTypeEncoding;

        // decide if Mic-E "longitude offset" must be added
        byte lonDeg = (byte)(Integer.parseInt(new String(longitude).substring(0, 3)));
        int longOffset = (lonDeg >= 10 && lonDeg < 100) ? 0 : 1;

        // generate Mic-E position and flags into the destination callsign, add N, W indicators
        ByteBuffer buffer = ByteBuffer.allocate(16);
        // 0, mic-e status
        buffer.put((byte)(latitude[0] + (isCustom ? 17 : 32) * ((miceMessageTypeEncoding >> 2) & 1)));
        // 1, mic-e status
        buffer.put((byte)(latitude[1] + (isCustom ? 17 : 32) * ((miceMessageTypeEncoding >> 1) & 1)));
        // 2, mic-e status
        if (latitude[2] == (byte)' ')
            buffer.put((miceMessageTypeEncoding & 1) == 1 ? isCustom ? (byte)'K' : (byte)'Z' : (byte)'L');
        else
            buffer.put((byte)(latitude[2] + (isCustom ? 17 : 32) * ((miceMessageTypeEncoding) & 1)));
        // 3, north/south
        if (latitude[3] == (byte)' ')
            buffer.put(latitude[6] == 'N' ? (byte)'Z' : (byte)'L');
        else
            buffer.put((byte)(latitude[3] + 32 * (latitude[6] == 'N' ? 1 : 0)));
        // 4, long offset
        if (latitude[4] == (byte)' ')
            buffer.put((longOffset & 1) == 1 ? (byte)'Z' : (byte)'L');
        else
            buffer.put((byte)(latitude[4] + 32 * (longOffset & 1)));
        // 5, west/east
        if (latitude[5] == (byte)' ')
            buffer.put(longitude[7] == 'W' ? (byte)'Z' : (byte)'L');
        else
            buffer.put((byte) (latitude[5] + 32 * (longitude[7] == 'W' ? 1 : 0)));

        // include E-Mic digipath if specified (!= 0)
        if (position.extDigipathSsid > 0) {
            buffer.put((byte)'-');
            buffer.put(Integer.toString(position.extDigipathSsid).getBytes());
        }

        // return
        buffer.flip();
        byte[] callsign = new byte[buffer.remaining()];
        buffer.get(callsign);
        return new String(callsign);
    }

    private byte[] generateInfo(Position position) {
        byte[] longitude = UnitTools.decimalToNmea(position.longitude, false).getBytes();

        ByteBuffer buffer = ByteBuffer.allocate(16);
        String longStr = new String(longitude);

        // encode Mic-E longitude into the information field
        byte lonDeg = (byte)(Integer.parseInt(longStr.substring(0, 3)));
        if (lonDeg >= 0 && lonDeg <= 9) lonDeg += (90 + 28);
        else if (lonDeg >= 10 && lonDeg <= 99) lonDeg += 28;
        else if (lonDeg >= 100 && lonDeg <= 109) lonDeg += 8;
        else lonDeg -= (44 + 28);
        buffer.put(lonDeg);

        byte lonMin = (byte)(Integer.parseInt(longStr.substring(3, 5)));
        if (lonMin >= 0 && lonMin <= 9) lonMin += (60 + 28);
        else lonMin += 28;
        buffer.put(lonMin);

        byte lonMinHun = (byte)(Integer.parseInt(longStr.substring(5, 7)));
        lonMinHun += 28;
        buffer.put(lonMinHun);

        // encode speed/course
        long speed = UnitTools.metersPerSecondToKnots(position.speedMetersPerSecond);
        byte speedHun = (byte)((speed / 10) + 28);
        buffer.put(speedHun);

        byte speedTen = (byte)(speed % 10);
        byte courseHun = (byte)(position.bearingDegrees / 100.0);
        buffer.put((byte)(speedTen * 10 + courseHun + 28));

        // encode bearing
        buffer.put((byte)(position.bearingDegrees % 100.0 + 28));

        // return
        buffer.flip();
        byte[] info = new byte[buffer.remaining()];
        buffer.get(info);
        return info;
    }
}
