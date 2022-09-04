package com.radio.codec2talkie.storage.station;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(indices = {@Index(value = {"srcCallsign"}, unique = true)})
public class StationItem {
    @NonNull
    @PrimaryKey
    private String srcCallsign;
    private long timestampEpoch;
    public String dstCallsign;
    private String maidenHead;
    public double latitude;
    public double longitude;
    public double altitudeMeters;
    public double bearingDegrees;
    public double speedMetersPerSecond;
    public String status;
    public String comment;
    public String symbolCode;
    public String logLine;
    public int privacyLevel;
    public double rangeMiles;
    public int directivityDeg;

    public StationItem(@NonNull String srcCallsign) {
        this.srcCallsign = srcCallsign;
    }

    public long getTimestampEpoch() { return timestampEpoch; }

    public String getSrcCallsign() { return srcCallsign; }

    public String getDstCallsign() { return dstCallsign; }

    public double getLatitude() { return latitude; }

    public double getLongitude() { return longitude; }

    public String getMaidenHead() { return maidenHead; }

    public double getAltitudeMeters() { return altitudeMeters; }

    public double getBearingDegrees() { return bearingDegrees; }

    public double getSpeedMetersPerSecond() { return speedMetersPerSecond; }

    public String getStatus() { return status; }

    public String getComment() { return comment; }

    public String getSymbolCode() { return symbolCode; }

    public String getLogLine() { return logLine; }

    public int getPrivacyLevel() { return privacyLevel; }

    public double getRangeMiles() { return rangeMiles; }

    public int getDirectivityDeg() { return directivityDeg; }

    public void setTimestampEpoch(long timestampEpoch) { this.timestampEpoch = timestampEpoch; }

    public void setSrcCallsign(String srcCallsign) { this.srcCallsign = srcCallsign; }

    public void setMaidenHead(String maidenHead) { this.maidenHead = maidenHead; }

    public void setDstCallsign(String dstCallsign) { this.dstCallsign = dstCallsign; }

    public void setLatitude(double latitude) { this.latitude = latitude; }

    public void setLongitude(double longitude) { this.longitude = longitude; }

    public void setAltitudeMeters(double altitudeMeters) { this.altitudeMeters = altitudeMeters; }

    public void setBearingDegrees(double bearingDegrees) { this.bearingDegrees = bearingDegrees; }

    public void setSpeedMetersPerSecond(double speedMetersPerSecond) { this.speedMetersPerSecond = speedMetersPerSecond; }

    public void setStatus(String status) { this.status = status; }

    public void setComment(String comment) { this.comment = comment; }

    public void setSymbolCode(String symbolCode) { this.symbolCode = symbolCode; }

    public void setPrivacyLevel(int privacyLevel) { this.privacyLevel = privacyLevel; }

    public void setLogLine(String logLine) { this.logLine = logLine; }

    public void setRangeMiles(double rangeMiles) { this.rangeMiles = rangeMiles; }

    public void setDirectivityDeg(int directivityDeg) { this.directivityDeg = directivityDeg; }

    public void updateFrom(StationItem stationItem) {
        setTimestampEpoch(stationItem.getTimestampEpoch());
        setDstCallsign(stationItem.getDstCallsign());
        // update position if known
        if (stationItem.getMaidenHead() != null) {
            setMaidenHead(stationItem.getMaidenHead());
            setLatitude(stationItem.getLatitude());
            setLongitude(stationItem.getLongitude());
            setAltitudeMeters(stationItem.getAltitudeMeters());
            setBearingDegrees(stationItem.getBearingDegrees());
            setSpeedMetersPerSecond(stationItem.getSpeedMetersPerSecond());
            setPrivacyLevel(stationItem.getPrivacyLevel());
            setRangeMiles(stationItem.getRangeMiles());
            setDirectivityDeg(stationItem.getDirectivityDeg());
        }
        if (stationItem.getStatus() != null)
            setStatus(stationItem.getStatus());
        if (stationItem.getComment() != null)
            setComment(stationItem.getComment());
        if (stationItem.getSymbolCode() != null)
            setSymbolCode(stationItem.getSymbolCode());
        if (stationItem.getLogLine() != null)
            setLogLine(stationItem.getLogLine());
    }

    @Override
    public boolean equals(Object o) {
        StationItem stationItem = (StationItem)o;
        return srcCallsign.equals(stationItem.getSrcCallsign()) &&
                timestampEpoch == stationItem.getTimestampEpoch() &&
                Objects.equals(comment, stationItem.getComment()) &&
                Objects.equals(dstCallsign, stationItem.getDstCallsign()) &&
                latitude == stationItem.getLatitude() &&
                longitude == stationItem.getLongitude();
    }
}
