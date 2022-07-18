package com.radio.codec2talkie.protocol.aprs;

import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;

public class AprsDataTextMessage implements AprsData {

    public String dstCallsign;
    public String textMessage;

    private boolean _isValid;

    @Override
    public void fromPosition(Position position) {
        _isValid = false;
    }

    @Override
    public void fromTextMessage(TextMessage textMessage) {
        this.dstCallsign = textMessage.dst;
        this.textMessage = textMessage.text;
        _isValid = true;
    }

    @Override
    public Position toPosition() {
        return null;
    }

    @Override
    public TextMessage toTextMessage() {
        TextMessage textMessage = new TextMessage();
        textMessage.dst = this.dstCallsign;
        textMessage.text = this.textMessage;
        return textMessage;
    }

    @Override
    public void fromBinary(byte[] infoData) {
        // TODO, implement
        _isValid = false;
    }

    @Override
    public byte[] toBinary() {
        return String.format(":%-9s:%s", dstCallsign, textMessage).getBytes();
    }

    @Override
    public boolean isValid() {
        return _isValid;
    }
}
