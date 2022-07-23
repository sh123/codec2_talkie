package com.radio.codec2talkie.protocol.position;

import android.location.Location;

import com.radio.codec2talkie.storage.position.PositionItem;
import com.radio.codec2talkie.tools.UnitTools;

public class Position {
    public long timestampEpochMs;
    public String srcCallsign;
    public String dstCallsign;
    public double latitude;
    public double longitude;
    public String maidenHead;
    public double altitudeMeters;
    public double bearingDegrees;
    public double speedMetersPerSecond;
    public String status;
    public String comment;
    public String symbolCode;
    public boolean isCompressed;
    public int privacyLevel;
    public int extDigipathSsid;
    public boolean isSpeedBearingEnabled;
    public boolean isAltitudeEnabled;
    public boolean hasBearing;
    public boolean hasAltitude;
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

    public PositionItem toPositionItem(boolean isTransmit) {
        PositionItem positionItem = new PositionItem();
        positionItem.setTimestampEpoch(System.currentTimeMillis());
        positionItem.setIsTransmit(isTransmit);
        positionItem.setSrcCallsign(srcCallsign);
        positionItem.setDstCallsign(dstCallsign);
        positionItem.setLatitude(latitude);
        positionItem.setLongitude(longitude);
        positionItem.setMaidenHead(maidenHead);
        positionItem.setAltitudeMeters(altitudeMeters);
        positionItem.setBearingDegrees(bearingDegrees);
        positionItem.setSpeedMetersPerSecond(speedMetersPerSecond);
        positionItem.setStatus(status);
        positionItem.setComment(comment);
        positionItem.setSymbolCode(symbolCode);
        positionItem.setPrivacyLevel(privacyLevel);
        return positionItem;
    }
}
