package com.radio.codec2talkie.protocol.aprs;

import com.radio.codec2talkie.protocol.aprs.tools.AprsTools;
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
        put("off_duty", 0b111);
        put("en_route", 0b110);
        put("in_service", 0b101);
        put("returning", 0b100);
        put("committed", 0b011);
        put("special", 0b010);
        put("priority", 0b001);
        put("emergency", 0b000);
    }};

    @Override
    public void fromPosition(Position position) {
        _isValid = false;
        _position = position;

        byte[] latitude = AprsTools.applyPrivacyOnUncompressedNmeaCoordinate(
                UnitTools.decimalToNmea(position.latitude, true),
                position.privacyLevel).getBytes();
        byte[] longitude = AprsTools.applyPrivacyOnUncompressedNmeaCoordinate(
                UnitTools.decimalToNmea(position.latitude, false),
                position.privacyLevel).getBytes();

        // latitude into the callsign
        _dstCallsign = generateCallsign(position, latitude, longitude);
        position.dstCallsign = _dstCallsign;

        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.putChar('`');

        // longitude
        byte lonDeg;
        byte lonMin;
        byte lonMinHun;

        // speed/course
        // symbol code
        // symbol table id
        // comment

        _isValid = true;
    }

    @Override
    public Position toPosition() {
        return _position;
    }

    @Override
    public void fromBinary(byte[] infoData) {
        _isValid = false;
        // TODO, needs dst callsign
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

    private String generateCallsign(Position position, byte[] latitude, byte[] longitude) {
        Integer miceMessageTypeEncoding = _miceMessageTypeMap.get(position.status);
        miceMessageTypeEncoding = miceMessageTypeEncoding == null ? 0b111 : miceMessageTypeEncoding;

        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.put((byte)((latitude[0] << 1) | ((miceMessageTypeEncoding >> 2) & 1)));
        buffer.put((byte)((latitude[1] << 1) | ((miceMessageTypeEncoding >> 1) & 1)));
        buffer.put((byte)((latitude[2] << 1) | ((miceMessageTypeEncoding) & 1)));
        buffer.put((byte)((latitude[3] << 1) | (latitude[6] == 'N' ? 1 : 0)));
        buffer.put((byte)((latitude[4] << 1) | 1)); // TODO
        buffer.put((byte)((latitude[5] << 1) | (longitude[6] == 'W' ? 1 : 0)));
        if (position.extDigipathSsid > 0) {
            buffer.putChar('-');
            buffer.put(Integer.toString(position.extDigipathSsid).getBytes());
        }

        buffer.flip();
        byte[] callsign = new byte[buffer.remaining()];
        buffer.get(callsign);
        return new String(callsign);
    }
}
