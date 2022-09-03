package com.radio.codec2talkie.storage.message;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(indices = {@Index(value = {"id", "srcCallsign"}, unique = true)})
public class MessageItem {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private long timestampEpoch;
    private String srcCallsign;
    private String dstCallsign;
    private String message;
    private boolean needsAck;
    private int ackNum;
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

    public String getDstCallsign() { return dstCallsign; }

    public String getMessage() { return message; }

    public boolean getNeedsAck() { return needsAck; }

    public int getAckNum() { return ackNum; }

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

    public void setDstCallsign(String dstCallsign) { this.dstCallsign = dstCallsign; }

    public void setMessage(String message) { this.message = message; }

    public void setNeedsAck(boolean needsAck) { this.needsAck = needsAck; }

    public void setAckNum(int ackNum) { this.ackNum = ackNum; }

    public void setIsTransmit(boolean isTransmit) { this.isTransmit = isTransmit; }
}

