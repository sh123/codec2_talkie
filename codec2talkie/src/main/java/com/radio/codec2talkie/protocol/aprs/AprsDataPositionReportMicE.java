package com.radio.codec2talkie.protocol.aprs;

import com.radio.codec2talkie.protocol.aprs.tools.AprsTools;
import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.tools.UnitTools;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class AprsDataPositionReportMicE implements AprsData {

    private String _dstCallsign;
    private Position _position;
    private byte[] _binary;
    private boolean _isValid;

    private final Map<String, Integer> _miceMessageTypeMap = new HashMap<String, Integer>() {{
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

        // comment
        buffer.put(position.comment.getBytes());

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
    public void fromBinary(byte[] infoData) {
        _isValid = false;
        // TODO, implement fromBinary, needs dst callsign
        // ByteBuffer buffer = ByteBuffer.wrap(infoData);
        // read latitude
        // read symbol table
        // read longitude
        // read symbol
        // read course/speed
        // read altitude (could be anywhere inside the comment)
        // read comment until the end
        // _isValid = true;
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

        // encode speed/cou8rse
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
