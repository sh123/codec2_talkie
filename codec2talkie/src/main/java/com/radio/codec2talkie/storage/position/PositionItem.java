package com.radio.codec2talkie.storage.position;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class PositionItem {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private long timestampEpoch;
    private boolean isTransmit;
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

    public double getLatitude() { return latitude; }

    public double getLongitude() { return longitude; }

    public String getMaidenHead() { return maidenHead; }

    public double getAltitudeMeters() { return altitudeMeters; }

    public double getBearingDegrees() { return bearingDegrees; }

    public double getSpeedMetersPerSecond() { return speedMetersPerSecond; };

    public String getStatus() { return status; }

    public String getComment() { return comment; };

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

    public void setLatitude(double latitude) { this.latitude = latitude; }

    public void setLongitude(double longitude) { this.longitude = longitude; }

    public void setMaidenHead(String maidenHead) { this.maidenHead = maidenHead; }

    public void setAltitudeMeters(double altitudeMeters) { this.altitudeMeters = altitudeMeters; }

    public void setBearingDegrees(double bearingDegrees) { this.bearingDegrees = bearingDegrees; }

    public void setSpeedMetersPerSecond(double speedMetersPerSecond) { this.speedMetersPerSecond = speedMetersPerSecond; }

    public void setStatus(String status) { this.status = status; }

    public void setComment(String comment) { this.comment = comment; }

    public void setSymbolCode(String symbolCode) { this.symbolCode = symbolCode; }

    public void setPrivacyLevel(int privacyLevel) { this.privacyLevel = privacyLevel; }

    public void setDirectivityDeg(int directivityDeg) { this.directivityDeg = directivityDeg; }

    public void setRangeMiles(double rangeMiles) { this.rangeMiles = rangeMiles; }

    public static boolean equalTo(PositionItem positionItem1, PositionItem positionItem2) {
        return positionItem1.getSrcCallsign().equals(positionItem2.getSrcCallsign()) &
               positionItem1.getDstCallsign().equals(positionItem2.getDstCallsign()) &
               positionItem1.getComment().equals(positionItem2.getComment()) &
               positionItem1.getStatus().equals(positionItem2.getStatus()) &
               positionItem1.getSymbolCode().equals(positionItem2.getSymbolCode()) &
               positionItem1.getAltitudeMeters() == positionItem2.getAltitudeMeters() &
               positionItem1.getBearingDegrees() == positionItem2.getBearingDegrees() &
               positionItem1.getSpeedMetersPerSecond() == positionItem2.getSpeedMetersPerSecond() &
               positionItem1.getLongitude() == positionItem2.getLongitude() &
               positionItem1.getAltitudeMeters() == positionItem2.getAltitudeMeters() &
               positionItem1.getBearingDegrees() == positionItem2.getBearingDegrees();
    }
}