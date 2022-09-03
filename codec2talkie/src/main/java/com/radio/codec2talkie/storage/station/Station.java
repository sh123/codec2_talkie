package com.radio.codec2talkie.storage.station;

public class Station {
    private long timestampEpoch;
    private String srcCallsign;
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

    public long getTimestampEpoch() { return timestampEpoch; }

    public String getSrcCallsign() { return srcCallsign; }

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
}
