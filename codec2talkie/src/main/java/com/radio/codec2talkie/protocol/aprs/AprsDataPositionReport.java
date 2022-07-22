package com.radio.codec2talkie.protocol.aprs;

import com.radio.codec2talkie.protocol.aprs.tools.AprsTools;
import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.tools.MathTools;
import com.radio.codec2talkie.tools.UnitTools;

import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        _position = new Position();
        if ((infoData[0] == '/' || infoData[0] == '\\') && fromCompressedBinary(infoData)) {
            _position.isCompressed = true;
            _isValid = true;

        } else if (fromUncompressedBinary(infoData)) {
            _position.isCompressed = false;
            _isValid = true;
        }
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
        buffer.put((byte)'=');
        buffer.put(getCompressedNmeaCoordinate(position));
        // compressed can hold either speed and bearing or altitude
        if (position.isSpeedBearingEnabled) {
            buffer.put((byte)(33 + (byte)(position.bearingDegrees / 4.0)));
            double compressedSpeed = MathTools.log(1.08, 1 + UnitTools.metersPerSecondToKnots(position.speedMetersPerSecond));
            buffer.put((byte)(33 + (byte)(compressedSpeed)));
            buffer.put((byte)'[');
        } else if (position.isAltitudeEnabled) {
            double compressedAltitude = MathTools.log(1.002, UnitTools.metersToFeet(position.altitudeMeters));
            buffer.put((byte)(33 + (byte)(compressedAltitude / 100)));
            buffer.put((byte)(33 + (byte)(compressedAltitude % 100)));
            buffer.put((byte)'S');
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
        buffer.put((byte)'=');
        buffer.put(getUncompressedNmeaCoordinate(position).getBytes());
        // put course altitude
        if (position.isSpeedBearingEnabled) {
            buffer.put(String.format(Locale.US, "%03d/%03d",
                    (int) position.bearingDegrees,
                    UnitTools.metersPerSecondToKnots(position.speedMetersPerSecond)).getBytes());
        }
        if (position.isAltitudeEnabled && position.altitudeMeters >= 0) {
            buffer.put(String.format(Locale.US, "/A=%06d",
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
        String latitude = AprsTools.applyPrivacyOnUncompressedNmeaCoordinate(
                UnitTools.decimalToDecimalNmea(position.latitude, true),
                position.privacyLevel);
        String longitude = AprsTools.applyPrivacyOnUncompressedNmeaCoordinate(
                UnitTools.decimalToDecimalNmea(position.longitude, false),
                position.privacyLevel);
        byte[] symbol = position.symbolCode.getBytes();
        return String.format(Locale.US, "%s%c%s%c", latitude, symbol[0], longitude, symbol[1]);
    }

    private boolean fromCompressedBinary(byte[] infoData) {
        ByteBuffer buffer = ByteBuffer.wrap(infoData);

        byte[] tail = new byte[buffer.remaining()];
        buffer.get(tail);
        String strTail = new String(tail);
        Pattern latLonPattern = Pattern.compile("^(\\\\|/)(\\S{4})(\\S{4})(\\S)(.\\S)(\\S)(.*)$");
        Matcher latLonMatcher = latLonPattern.matcher(strTail);
        if (!latLonMatcher.matches()) return false;

        String table = latLonMatcher.group(1);
        String latitude = latLonMatcher.group(2);
        String longitude = latLonMatcher.group(3);
        String symbol = latLonMatcher.group(4);
        String altSpeed = latLonMatcher.group(5);
        String tValue = latLonMatcher.group(6);
        String comment = latLonMatcher.group(7);

        _position.symbolCode = String.format("%s%s", table, symbol);
        if (latitude == null) return false;
        _position.latitude = getUncompressedCoordinate(latitude.getBytes(), true);
        if (longitude == null) return false;
        _position.longitude = getUncompressedCoordinate(longitude.getBytes(), false);
        if (comment == null)
            _position.comment = "";
        else
            _position.comment = comment;

        if (altSpeed == null) return false;
        if (tValue == null) return false;

        byte tByte = (byte) ((byte)tValue.charAt(0) - 33);
        int tByteNmeaSource = ((tByte >> 3) & 0x3);
        byte cByte = (byte)altSpeed.charAt(0);
        byte sByte = (byte)altSpeed.charAt(1);

        _position.hasSpeed = false;
        _position.hasBearing = false;
        _position.isSpeedBearingEnabled = false;
        _position.hasAltitude = false;
        _position.isAltitudeEnabled = false;

        // no course/speed
        if (cByte == ' ') return true;

        // altitude
        int NMEA_SRC_GGA = 0x02;
        int NMEA_SRC_RMC = 0x03;
        if (tByteNmeaSource == NMEA_SRC_GGA) {
            _position.altitudeMeters = UnitTools.feetToMeters((long) Math.pow(1.002, (cByte - 33) * 91 + (sByte - 33)));
            _position.hasAltitude = true;
            _position.isAltitudeEnabled = true;
        }
        // compressed course/speed
        else if (tByteNmeaSource == NMEA_SRC_RMC && cByte >= '!' && cByte <= 'z') {
            _position.bearingDegrees = 4.0f * (cByte - 33);
            _position.speedMetersPerSecond = (float)Math.pow(1.08, sByte - 33) - 1.0f;
            _position.hasSpeed = true;
            _position.hasBearing = true;
            _position.isSpeedBearingEnabled = true;
        }
        // radio range
        else if (cByte == '{') {
            // TODO, implement
            double rangeMiles = 2 * Math.pow(1.08, sByte);
        }
        return true;
    }

    private boolean fromUncompressedBinary(byte[] infoData) {
        ByteBuffer buffer = ByteBuffer.wrap(infoData);

        // read latitude/symbol_table/longitude/symbol
        byte[] tail = new byte[buffer.remaining()];
        buffer.get(tail);
        String strTail = new String(tail);
        Pattern latLonPattern = Pattern.compile("^(\\d{4}[.]\\d{2})(N|S)([/\\\\])(\\d{5}[.]\\d{2})(E|W)(\\S)(.+)$");
        Matcher latLonMatcher = latLonPattern.matcher(strTail);
        if (!latLonMatcher.matches()) return false;

        String lat = latLonMatcher.group(1);
        String latSuffix = latLonMatcher.group(2);
        if (lat == null || latSuffix == null) return false;
        _position.latitude = UnitTools.nmeaToDecimal(lat, latSuffix);
        String table = latLonMatcher.group(3);
        String lon = latLonMatcher.group(4);
        String lonSuffix = latLonMatcher.group(5);
        if (lon == null || lonSuffix == null) return false;
        _position.longitude = UnitTools.nmeaToDecimal(lon, lonSuffix);
        String symbol = latLonMatcher.group(6);
        _position.symbolCode = String.format("%s%s", table, symbol);
        strTail = latLonMatcher.group(7);
        if (strTail == null) return false;

        // read course/speed
        Pattern courseSpeedPattern = Pattern.compile("^(\\d{3})/(\\d{3})(.+)$");
        Matcher courseSpeedMatcher = courseSpeedPattern.matcher(strTail);
        if (courseSpeedMatcher.matches()) {
            String course = courseSpeedMatcher.group(1);
            String speed = courseSpeedMatcher.group(2);
            strTail = courseSpeedMatcher.group(3);
            if (strTail == null || speed == null || course == null) return false;
            _position.bearingDegrees = Float.parseFloat(course);
            _position.speedMetersPerSecond = UnitTools.knotsToMetersPerSecond(Long.parseLong(speed));
            _position.isSpeedBearingEnabled = true;
            _position.hasBearing = true;
            _position.hasSpeed = true;
        } else {
            _position.isSpeedBearingEnabled = false;
            _position.hasBearing = false;
            _position.hasSpeed = false;
        }
        // read altitude (could be anywhere inside the comment)
        Pattern altitudePattern = Pattern.compile("/A=(\\d{6})");
        Matcher altitudeMatcher = altitudePattern.matcher(strTail);
        if (altitudeMatcher.matches()) {
            String altitude = altitudeMatcher.group(1);
            if (altitude == null) return false;
            strTail = altitudeMatcher.replaceAll("");
            _position.altitudeMeters = UnitTools.feetToMeters(Long.parseLong(altitude));
            _position.isAltitudeEnabled = true;
            _position.hasAltitude = true;
        } else {
            _position.isAltitudeEnabled = false;
            _position.hasAltitude = false;
        }
        // read comment until the end
        _position.comment = strTail;
        return true;
    }

    private double getUncompressedCoordinate(byte[] data, boolean isLatitude) {
        double v = (data[0] - 33) * 91 * 91 * 91 +
                (data[1] - 33) * 91 * 91 +
                (data[2] - 33) * 91 +
                (data[2] - 33);
        if (isLatitude)
            return 90 - v / 380926.0;
        return -180 + v / 190463.0;
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
}
