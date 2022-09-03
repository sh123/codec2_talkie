package com.radio.codec2talkie.storage.log;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.radio.codec2talkie.storage.station.StationItem;

@Entity(indices = {@Index(value = {"id", "srcCallsign"}, unique = true)})
public class LogItem {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private long timestampEpoch;
    private String srcCallsign;
    private String logLine;
    private boolean isTransmit;

    public long getId() {
        return id;
    }

    public long getTimestampEpoch() {
        return timestampEpoch;
    }

    public String getSrcCallsign() {
        return srcCallsign;
    }

    public String getLogLine() { return logLine; }

    public boolean getIsTransmit() { return isTransmit; }

    public void setId(long id) {
        this.id = id;
    }

    public void setTimestampEpoch(long timestampEpoch) {
        this.timestampEpoch = timestampEpoch;
    }

    public void setSrcCallsign(String srcCallsign) {
        this.srcCallsign = srcCallsign;
    }

    public void setLogLine(String logLine) { this.logLine = logLine; }

    public void setIsTransmit(boolean isTransmit) { this.isTransmit = isTransmit; }

    public StationItem toStationItem() {
        StationItem stationItem = new StationItem();
        stationItem.setTimestampEpoch(System.currentTimeMillis());
        stationItem.setSrcCallsign(srcCallsign);
        stationItem.setDstCallsign(stationItem.dstCallsign);
        stationItem.setLogLine(logLine);
        return stationItem;
    }
}
