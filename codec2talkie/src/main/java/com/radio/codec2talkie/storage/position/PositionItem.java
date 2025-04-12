package com.radio.codec2talkie.storage.position;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(indices = {@Index(value = {"id", "srcCallsign"}, unique = true)})
public class PositionItem {

    private static final double MIN_COORDINATE_CHANGE_DELTA = 0.003;

    @PrimaryKey(autoGenerate = true)
    private long id;
    private long timestampEpoch;
    private boolean isTransmit;
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
    public int privacyLevel;
    public double rangeMiles;
    public int directivityDeg;

    public long getId() {
        return id;
    }

    public long getTimestampEpoch() {
        return timestampEpoch;
    }

    public String getSrcCallsign() {
        return srcCallsign;
    }

    public String getDstCallsign() { return dstCallsign; }

    public String getDigipath() { return digipath; }

    public double getLatitude() { return latitude; }

    public double getLongitude() { return longitude; }

    public String getMaidenHead() { return maidenHead; }

    public double getAltitudeMeters() { return altitudeMeters; }

    public double getBearingDegrees() { return bearingDegrees; }

    public double getSpeedMetersPerSecond() { return speedMetersPerSecond; }

    public String getStatus() { return status; }

    public String getComment() { return comment; }

    public String getDeviceIdDescription() { return deviceIdDescription; }

    public String getSymbolCode() { return symbolCode; }

    public int getPrivacyLevel() { return privacyLevel; }

    public boolean getIsTransmit() { return isTransmit; }

    public int getDirectivityDeg() { return directivityDeg; }

    public double getRangeMiles() { return rangeMiles; }

    public void setId(long id) {
        this.id = id;
    }

    public void setTimestampEpoch(long timestampEpoch) {
        this.timestampEpoch = timestampEpoch;
    }

    public void setIsTransmit(boolean isTransmit) { this.isTransmit = isTransmit; }

    public void setSrcCallsign(String srcCallsign) { this.srcCallsign = srcCallsign; }

    public void setDstCallsign(String dstCallsign) { this.dstCallsign = dstCallsign; }

    public void setDigipath(String digipath) { this.digipath = digipath; }

    public void setLatitude(double latitude) { this.latitude = latitude; }

    public void setLongitude(double longitude) { this.longitude = longitude; }

    public void setMaidenHead(String maidenHead) { this.maidenHead = maidenHead; }

    public void setAltitudeMeters(double altitudeMeters) { this.altitudeMeters = altitudeMeters; }

    public void setBearingDegrees(double bearingDegrees) { this.bearingDegrees = bearingDegrees; }

    public void setSpeedMetersPerSecond(double speedMetersPerSecond) { this.speedMetersPerSecond = speedMetersPerSecond; }

    public void setStatus(String status) { this.status = status; }

    public void setComment(String comment) { this.comment = comment; }

    public void setDeviceIdDescription(String deviceIdDescription) { this.deviceIdDescription = deviceIdDescription; }

    public void setMinCoordinateChangeDelta(String deviceIdDescription) { this.deviceIdDescription = deviceIdDescription; }

    public void setSymbolCode(String symbolCode) { this.symbolCode = symbolCode; }

    public void setPrivacyLevel(int privacyLevel) { this.privacyLevel = privacyLevel; }

    public void setDirectivityDeg(int directivityDeg) { this.directivityDeg = directivityDeg; }

    public void setRangeMiles(double rangeMiles) { this.rangeMiles = rangeMiles; }

    @Override
    public boolean equals(Object o) {
        PositionItem positionItem = (PositionItem) o;
        return getSrcCallsign().equals(positionItem.getSrcCallsign()) &
               getIsTransmit() == positionItem.getIsTransmit() &&
               Math.abs(getLongitude() - positionItem.getLongitude()) <= MIN_COORDINATE_CHANGE_DELTA &
               Math.abs(getLatitude() - positionItem.getLatitude()) <= MIN_COORDINATE_CHANGE_DELTA;
    }
}