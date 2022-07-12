package com.radio.codec2talkie.storage.message;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class MessageItem {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private long timestampEpoch;
    private String srcCallsign;
    private String dstCallsign;
    private String message;
    private boolean needsAck;
    private int ackNum;

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
}

