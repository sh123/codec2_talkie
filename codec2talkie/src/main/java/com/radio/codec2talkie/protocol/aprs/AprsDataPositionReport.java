package com.radio.codec2talkie.protocol.aprs;

import android.util.Log;

import com.radio.codec2talkie.protocol.aprs.tools.AprsTools;
import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.tools.DeviceIdTools;
import com.radio.codec2talkie.tools.MathTools;
import com.radio.codec2talkie.tools.TextTools;
import com.radio.codec2talkie.tools.UnitTools;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AprsDataPositionReport implements AprsData {
    private static final String TAG = AprsData.class.getSimpleName();

    private Position _position;
    private byte[] _binary;
    private boolean _isValid;

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
    public void fromBinary(String srcCallsign, String dstCallsign, String digipath, byte[] infoData) {
        _isValid = false;
        _position = new Position();
        _position.srcCallsign = srcCallsign;
        _position.dstCallsign = dstCallsign;
        _position.digipath = digipath;
        _position.status = "";
        _position.comment = "";
        _position.deviceIdDescription = "";
        _position.privacyLevel = 0;
        if ((infoData[0] == '/' || infoData[0] == '\\') && fromCompressedBinary(infoData)) {
            _position.isCompressed = true;
            _isValid = true;

        } else if (fromUncompressedBinary(infoData)) {
            _position.isCompressed = false;
            _isValid = true;
        }
        AprsCallsign dstAprsCallsign = new AprsCallsign(dstCallsign);
        if (dstAprsCallsign.isSoftware()) {
            _position.deviceIdDescription = DeviceIdTools.getDeviceDescription(dstCallsign);
        }
        if (_isValid)
            _position.maidenHead = UnitTools.decimalToMaidenhead(_position.latitude, _position.longitude);
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
        buffer.put(position.comment.getBytes(StandardCharsets.UTF_8));
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
        buffer.put(position.comment.getBytes(StandardCharsets.UTF_8));
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
        String strTail = new String(tail, StandardCharsets.UTF_8);
        Pattern latLonPattern = Pattern.compile("^([\\\\/])(\\S{4})(\\S{4})(\\S)(.\\S)?(\\S)?(.*)$", Pattern.DOTALL);
        Matcher latLonMatcher = latLonPattern.matcher(strTail);
        if (!latLonMatcher.matches()) {
            Log.w(TAG, "cannot match compressed aprs data");
            return false;
        }

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
        if (comment != null) {
            _position.comment = TextTools.stripNulls(comment);
        }

        _position.hasSpeed = false;
        _position.hasBearing = false;
        _position.isSpeedBearingEnabled = false;
        _position.hasAltitude = false;
        _position.isAltitudeEnabled = false;

        if (altSpeed == null || tValue == null) {
            return true;
        }

        byte tByte = (byte) ((byte)tValue.charAt(0) - 33);
        int tByteNmeaSource = ((tByte >> 3) & 0x3);
        byte cByte = (byte)altSpeed.charAt(0);
        byte sByte = (byte)altSpeed.charAt(1);

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

        // sometimes altitude is coming from comment event for compressed packets
        _position.comment = parseAltitude(_position.comment);
        return true;
    }

    private boolean fromUncompressedBinary(byte[] infoData) {
        ByteBuffer buffer = ByteBuffer.wrap(infoData);

        // read latitude/symbol_table/longitude/symbol
        byte[] tail = new byte[buffer.remaining()];
        buffer.get(tail);
        String strTail = new String(tail, StandardCharsets.UTF_8);
        Pattern latLonPattern = Pattern.compile(
                "^" +
                "(?:.*)?" +                         // optional timestamp
                "([\\d ]{4}[.][\\d ]{2})([NS])" +   // latitude "
                "(\\S)" +                           // symbol table
                "([\\d ]{5}[.][\\d ]{2})([EW])" +   // longitude
                "(\\S)(.+)?" +                      // tail (speed/bearing/altitude/comment)
                "$", Pattern.DOTALL);

        Matcher latLonMatcher = latLonPattern.matcher(strTail);
        if (!latLonMatcher.matches()) return false;

        String lat = latLonMatcher.group(1);
        String latSuffix = latLonMatcher.group(2);
        if (lat == null || latSuffix == null) return false;
        _position.privacyLevel = TextTools.countChars(lat, ' ');
        // NOTE, ambiguity, replace with 0
        lat = lat.replace(' ', '0');
        try {
            _position.latitude = UnitTools.nmeaToDecimal(lat, latSuffix);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return false;
        }
        String table = latLonMatcher.group(3);
        String lon = latLonMatcher.group(4);
        String lonSuffix = latLonMatcher.group(5);
        if (lon == null || lonSuffix == null) return false;
        // NOTE, ambiguity, replace with 0
        lon = lon.replace(' ', '0');
        try {
            _position.longitude = UnitTools.nmeaToDecimal(lon, lonSuffix);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return false;
        }
        String symbol = latLonMatcher.group(6);
        _position.symbolCode = String.format("%s%s", table, symbol);
        strTail = latLonMatcher.group(7);
        if (strTail == null) return true;

        // read course/speed
        Pattern courseSpeedPattern = Pattern.compile("^(\\d{3})/(\\d{3})(.*)?$", Pattern.DOTALL);
        Matcher courseSpeedMatcher = courseSpeedPattern.matcher(strTail);
        if (courseSpeedMatcher.matches()) {
            String course = courseSpeedMatcher.group(1);
            String speed = courseSpeedMatcher.group(2);
            strTail = courseSpeedMatcher.group(3);
            if (speed != null && course != null) {
                _position.bearingDegrees = Float.parseFloat(course);
                _position.speedMetersPerSecond = UnitTools.knotsToMetersPerSecond(Long.parseLong(speed));
                _position.isSpeedBearingEnabled = true;
                _position.hasBearing = true;
                _position.hasSpeed = true;
            }
        } else {
            _position.isSpeedBearingEnabled = false;
            _position.hasBearing = false;
            _position.hasSpeed = false;
        }
        if (strTail == null) return true;

        // try parse altitude
        strTail = parseAltitude(strTail);

        // read PHG range
        Pattern phgPattern = Pattern.compile("^.*(PHG\\d{4}).*$", Pattern.DOTALL);
        Matcher phgMatcher = phgPattern.matcher(strTail);
        if (phgMatcher.matches()) {
            String phg = phgMatcher.group(1);
            if (phg != null) {
                strTail = strTail.replaceAll(phg, "");
                _position.directivityDeg = AprsTools.phgToDirectivityDegrees(phg);
                _position.rangeMiles = AprsTools.phgToRangeMiles(phg);
            }
        }

        // read RNG range
        Pattern rngPattern = Pattern.compile("^.*(RNG\\d{4}).*$", Pattern.DOTALL);
        Matcher rngMatcher = rngPattern.matcher(strTail);
        if (rngMatcher.matches()) {
            String rng = rngMatcher.group(1);
            if (rng != null) {
                strTail = strTail.replaceAll(rng, "");
                _position.rangeMiles = Double.parseDouble(rng.substring(3));
                _position.directivityDeg = 0;
            }
        }

        // read comment until the end
        _position.comment = TextTools.stripNulls(strTail);
        return true;
    }

    private String parseAltitude(String strData) {
        String strTail = strData;
        // read altitude (could be anywhere inside the comment)
        Pattern altitudePattern = Pattern.compile("^.*(/A=\\d{6}).*$", Pattern.DOTALL);
        Matcher altitudeMatcher = altitudePattern.matcher(strTail);
        if (altitudeMatcher.matches()) {
            String altitude = altitudeMatcher.group(1);
            if (altitude != null) {
                strTail = strTail.replaceAll(altitude, "");
                altitude = altitude.split("=")[1];
                _position.altitudeMeters = UnitTools.feetToMeters(Long.parseLong(altitude));
                _position.isAltitudeEnabled = true;
                _position.hasAltitude = true;
            }
        } else {
            _position.isAltitudeEnabled = false;
            _position.hasAltitude = false;
        }
        return strTail;
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
