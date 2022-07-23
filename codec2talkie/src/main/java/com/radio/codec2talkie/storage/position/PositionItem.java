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

    public void setId(long id) {
        this.id = id;
    }

    public void setTimestampEpoch(long timestampEpoch) {
        this.timestampEpoch = timestampEpoch;
    }

    public void setIsTransmit(boolean isTransmit) { this.isTransmit = isTransmit; }

    public void  setSrcCallsign(String srcCallsign) { this.srcCallsign = srcCallsign; }

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
}