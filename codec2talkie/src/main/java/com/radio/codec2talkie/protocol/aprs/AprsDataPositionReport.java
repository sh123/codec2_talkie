package com.radio.codec2talkie.protocol.aprs;

import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.tools.UnitTools;

import java.nio.ByteBuffer;
import java.util.Locale;

public class AprsDataPositionReport implements AprsData {

    private Position _position;
    private byte[] _binary;
    private boolean _isValid;

    @Override
    public void fromPosition(Position position) {
        _isValid = false;
        _position = position;
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.putChar('!');
        buffer.put(getUncompressedNmeaCoordinate(position).getBytes());
        // put altitude, course
        if (position.isAltitudeEnabled && position.altitudeMeters >= 0) {
            buffer.put(String.format(Locale.US, "/A=%05d", UnitTools.metersToFeet(position.altitudeMeters)).getBytes());
        }
        if (position.isSpeedBearingEnabled) {
        }
        buffer.put(position.comment.getBytes());
        buffer.flip();
        _binary = new byte[buffer.remaining()];
        buffer.get(_binary);
        _isValid = true;
    }

    @Override
    public Position toPosition() {
        return _position;
    }

    @Override
    public void fromBinary(byte[] infoData) {
        _isValid = false;
        ByteBuffer buffer = ByteBuffer.wrap(infoData);
        // read latitude
        // read symbol table
        // read longitude
        // read symbol
        // read altitude, course
        // read comment until the end
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

    private String getUncompressedNmeaCoordinate(Position position) {
        String latitude = applyPrivacyOnNmeaCoordinate(
                UnitTools.decimalToNmea(position.latitude, true),
                position.privacyLevel);
        String longitude = applyPrivacyOnNmeaCoordinate(
                UnitTools.decimalToNmea(position.latitude, false),
                position.privacyLevel);
        byte[] symbol = position.symbolCode.getBytes();
        return String.format(Locale.US, "%s%c%s%c", latitude, symbol[0], longitude, symbol[1]);
    }

    public static String applyPrivacyOnNmeaCoordinate(String nmeaCoordinate, int privacyLevel) {
        byte [] buffer = nmeaCoordinate.getBytes();
        int level = 0;
        for (int i = buffer.length - 2; i > 0 && level < privacyLevel; i--) {
            if (buffer[i] == '.') continue;
            buffer[i] = ' ';
            level++;
        }
        return new String(buffer);
    }
}
