package com.radio.codec2talkie.protocol.message;

import com.radio.codec2talkie.storage.message.MessageItem;

import java.util.Locale;

public class TextMessage {
    public String src;
    public String dst;
    public String digipath;
    public String text;
    public Integer ackId;

    public MessageItem toMessageItem(boolean isTransmit) {
        MessageItem messageItem = new MessageItem();
        messageItem.setTimestampEpoch(System.currentTimeMillis());
        messageItem.setIsTransmit(isTransmit);
        messageItem.setSrcCallsign(this.src);
        messageItem.setDstCallsign(this.dst);
        messageItem.setMessage(this.text);
        messageItem.setAckId(this.ackId);
        messageItem.setIsAcknowledged(false);
        messageItem.setRetryCnt(0);
        return messageItem;
    }

    public boolean isAck() {
        return this.text != null &&
                this.text.toLowerCase(Locale.ROOT).equals("ack") &&
                this.ackId > 0;
    }

    public boolean isRej() {
        return this.text != null &&
                this.text.toLowerCase(Locale.ROOT).equals("rej") &&
                this.ackId > 0;
    }

    public boolean isAutoReply() {
        return isAck() || isRej();
    }
}
