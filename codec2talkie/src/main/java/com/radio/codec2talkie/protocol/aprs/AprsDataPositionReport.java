package com.radio.codec2talkie.protocol.aprs;

import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.tools.MathTools;
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
        _binary = position.isCompressed
                ? generateCompressedInfo(position)
                : generateUncompressedInfo(position);
        _isValid = true;
    }

    @Override
    public Position toPosition() {
        return _position;
    }

    @Override
    public void fromBinary(byte[] infoData) {
        _isValid = false;
        // TODO
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

    private byte[] generateCompressedInfo(Position position) {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.putChar('!');
        buffer.put(getCompressedNmeaCoordinate(position));
        // compressed can hold either speed and bearing or altitude
        if (position.isSpeedBearingEnabled) {
            buffer.put((byte)(33 + (byte)(position.bearingDegrees / 4.0)));
            double compressedSpeed = MathTools.log(1.08, 1 + UnitTools.metersPerSecondToKnots(position.speedMetersPerSecond));
            buffer.put((byte)(33 + (byte)(compressedSpeed)));
            buffer.putChar('[');
        } else if (position.isAltitudeEnabled) {
            double compressedAltitude = MathTools.log(1.002, UnitTools.metersToFeet(position.altitudeMeters));
            buffer.put((byte)(33 + (byte)(compressedAltitude / 100)));
            buffer.put((byte)(33 + (byte)(compressedAltitude % 100)));
            buffer.putChar('T');
        } else {
            buffer.put(" sT".getBytes());
        }
        buffer.put(position.comment.getBytes());
        // return
        buffer.flip();
        byte [] binaryInfo = new byte[buffer.remaining()];
        buffer.get(binaryInfo);
        return binaryInfo;
    }

    private byte[] generateUncompressedInfo(Position position) {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.putChar('!');
        buffer.put(getUncompressedNmeaCoordinate(position).getBytes());
        // put course altitude
        if (position.isSpeedBearingEnabled) {
            buffer.put(String.format(Locale.US, "%03d/%03d",
                    (int) position.bearingDegrees,
                    UnitTools.metersPerSecondToKnots(position.speedMetersPerSecond)).getBytes());
        }
        if (position.isAltitudeEnabled && position.altitudeMeters >= 0) {
            buffer.put(String.format(Locale.US, "/A=%05d",
                    UnitTools.metersToFeet(position.altitudeMeters)).getBytes());
        }
        buffer.put(position.comment.getBytes());
        // return
        buffer.flip();
        byte [] binaryInfo = new byte[buffer.remaining()];
        buffer.get(binaryInfo);
        return binaryInfo;
    }

    private String getUncompressedNmeaCoordinate(Position position) {
        String latitude = applyPrivacyOnUncompressedNmeaCoordinate(
                UnitTools.decimalToNmea(position.latitude, true),
                position.privacyLevel);
        String longitude = applyPrivacyOnUncompressedNmeaCoordinate(
                UnitTools.decimalToNmea(position.latitude, false),
                position.privacyLevel);
        byte[] symbol = position.symbolCode.getBytes();
        return String.format(Locale.US, "%s%c%s%c", latitude, symbol[0], longitude, symbol[1]);
    }

    private byte[] getCompressedNmeaCoordinate(Position position) {
        byte[] symbol = position.symbolCode.getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(10);
        // symbol table
        buffer.put(symbol[0]);
        // latitude
        double lat = Math.abs(380926 * (position.latitude - 90.0));
        buffer.put((byte)(33 + (byte)(lat / (91 * 91 * 91))));
        lat %= (91 * 91 * 91);
        buffer.put((byte)(33 + (byte)(lat / (91 * 91))));
        lat %= (91 * 91);
        buffer.put((byte)(33 + (byte)(lat / (91))));
        lat %= (91);
        buffer.put((byte)(33 + (byte)lat));
        // longitude
        double lon = Math.abs(190463 * (position.longitude + 180.0));
        buffer.put((byte)(33 + (byte)(lon / (91 * 91 * 91))));
        lon %= (91 * 91 * 91);
        buffer.put((byte)(33 + (byte)(lon / (91 * 91))));
        lon %= (91 * 91);
        buffer.put((byte)(33 + (byte)(lon / (91))));
        lon %= (91);
        buffer.put((byte)(33 + (byte)lon));
        // symbol
        buffer.put(symbol[1]);
        // return
        buffer.flip();
        byte [] binaryCoordinate = new byte[buffer.remaining()];
        buffer.get(binaryCoordinate);
        return binaryCoordinate;
    }

    public static String applyPrivacyOnUncompressedNmeaCoordinate(String nmeaCoordinate, int privacyLevel) {
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
