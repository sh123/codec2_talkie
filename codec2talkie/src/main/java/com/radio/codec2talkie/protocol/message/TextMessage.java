package com.radio.codec2talkie.protocol.message;

import com.radio.codec2talkie.storage.message.MessageItem;

public class TextMessage {
    public String src;
    public String dst;
    public String text;

    public MessageItem toMessageItem(boolean isTransmit) {
        MessageItem messageItem = new MessageItem();
        messageItem.setTimestampEpoch(System.currentTimeMillis());
        messageItem.setNeedsAck(false); // TODO
        messageItem.setIsTransmit(isTransmit);
        messageItem.setSrcCallsign(this.src);
        messageItem.setDstCallsign(this.dst);
        messageItem.setMessage(this.text);
        return messageItem;
    }
}
