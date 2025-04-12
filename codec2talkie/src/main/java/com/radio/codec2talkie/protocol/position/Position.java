package com.radio.codec2talkie.protocol.position;

import android.location.Location;

import com.radio.codec2talkie.storage.position.PositionItem;
import com.radio.codec2talkie.storage.station.StationItem;
import com.radio.codec2talkie.tools.UnitTools;

public class Position {

    public final static double DEFAULT_RANGE_MILES = 6.0;

    public long timestampEpochMs;
    public String srcCallsign;
    public String dstCallsign;
    public String digipath;
    public double latitude;
    public double longitude;
    public String maidenHead;
    public double altitudeMeters;
    public double bearingDegrees;
    public double speedMetersPerSecond;
    public String status;
    public String comment;
    public String deviceIdDescription;
    public String symbolCode;
    public boolean isCompressed;
    public int privacyLevel;
    public int extDigipathSsid;
    public boolean isSpeedBearingEnabled;
    public boolean isAltitudeEnabled;
    public boolean hasBearing;
    public boolean hasAltitude;
    public double rangeMiles;
    public int directivityDeg;
    public boolean hasSpeed;

    public static Position fromLocation(Location location) {
        Position position = new Position();
        position.latitude = location.getLatitude();
        position.longitude = location.getLongitude();
        position.bearingDegrees = location.getBearing();
        position.altitudeMeters = location.getAltitude();
        position.speedMetersPerSecond = location.getSpeed();
        position.timestampEpochMs = location.getTime();
        position.hasBearing = location.hasBearing();
        position.hasAltitude = location.hasAltitude();
        position.hasSpeed = location.hasSpeed();
        position.maidenHead = UnitTools.decimalToMaidenhead(position.latitude, position.longitude);
        position.rangeMiles = 0.0;
        position.directivityDeg = 0;    // 0 - omni
        return position;
    }

    public double distanceTo(Position position) {
        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(position.latitude - latitude);
        double lonDistance = Math.toRadians(position.longitude - longitude);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(position.latitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = altitudeMeters - position.longitude;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

    public static double distanceTo(double lat1, double lon1, double alt1, double lat2, double lon2, double alt2) {
        Position pos1 = new Position();
        pos1.latitude = lat1;
        pos1.longitude = lon1;
        pos1.altitudeMeters = alt1;

        Position pos2 = new Position();
        pos2.latitude = lat2;
        pos2.longitude = lon2;
        pos2.altitudeMeters = alt2;

        return pos1.distanceTo(pos2);
    }

    public static String bearing(double lat1, double lon1, double lat2, double lon2) {
        double radians = Math.atan2(lon2 - lon1, lat2 - lat1);
        double degrees = radians * (180.0 / Math.PI);
        String[] dirNames = {"N", "NE", "E", "SE", "S", "SW", "W", "NW", "N"};
        int dirIndex = (int) Math.round(degrees / 45);
        if (dirIndex < 0) {
            dirIndex = dirIndex + 8;
        }
        return dirNames[dirIndex];
    }

    public PositionItem toPositionItem(boolean isTransmit) {
        PositionItem positionItem = new PositionItem();
        positionItem.setTimestampEpoch(System.currentTimeMillis());
        positionItem.setIsTransmit(isTransmit);
        positionItem.setSrcCallsign(srcCallsign);
        positionItem.setDstCallsign(dstCallsign);
        positionItem.setDigipath(digipath);
        positionItem.setLatitude(latitude);
        positionItem.setLongitude(longitude);
        positionItem.setMaidenHead(maidenHead);
        positionItem.setAltitudeMeters(altitudeMeters);
        positionItem.setBearingDegrees(bearingDegrees);
        positionItem.setSpeedMetersPerSecond(speedMetersPerSecond);
        positionItem.setStatus(status);
        positionItem.setComment(comment);
        positionItem.setDeviceIdDescription(deviceIdDescription);
        positionItem.setSymbolCode(symbolCode);
        positionItem.setPrivacyLevel(privacyLevel);
        positionItem.setDirectivityDeg(directivityDeg);
        positionItem.setRangeMiles(rangeMiles);
        return positionItem;
    }

    public StationItem toStationItem() {
        StationItem stationItem = new StationItem(srcCallsign);
        stationItem.setTimestampEpoch(System.currentTimeMillis());
        stationItem.setDstCallsign(dstCallsign);
        stationItem.setDigipath(digipath);
        stationItem.setLatitude(latitude);
        stationItem.setLongitude(longitude);
        stationItem.setMaidenHead(maidenHead);
        stationItem.setAltitudeMeters(altitudeMeters);
        stationItem.setBearingDegrees(bearingDegrees);
        stationItem.setSpeedMetersPerSecond(speedMetersPerSecond);
        stationItem.setStatus(status);
        stationItem.setComment(comment);
        stationItem.setDeviceIdDescription(deviceIdDescription);
        stationItem.setSymbolCode(symbolCode);
        stationItem.setPrivacyLevel(privacyLevel);
        stationItem.setDirectivityDeg(directivityDeg);
        stationItem.setRangeMiles(rangeMiles);
        return stationItem;
    }
}
