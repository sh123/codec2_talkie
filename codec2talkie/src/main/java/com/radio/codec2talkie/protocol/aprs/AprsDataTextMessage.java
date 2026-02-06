package com.radio.codec2talkie.protocol.aprs;

import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AprsDataTextMessage implements AprsData {

    public String srcCallsign;
    public String dstCallsign;
    public String digipath;
    public String textMessage;
    public String ackId;

    private boolean _isValid;

    @Override
    public boolean isPositionReport() {
        return false;
    }

    @Override
    public boolean isTextMessage() {
        return true;
    }

    @Override
    public void fromPosition(Position position) {
        _isValid = false;
    }

    @Override
    public void fromTextMessage(TextMessage textMessage) {
        this.dstCallsign = textMessage.dst;
        this.textMessage = textMessage.text;
        this.digipath = textMessage.digipath;
        this.ackId = textMessage.ackId;
        _isValid = true;
    }

    @Override
    public Position toPosition() {
        return null;
    }

    @Override
    public TextMessage toTextMessage() {
        TextMessage textMessage = new TextMessage();
        textMessage.src = this.srcCallsign;
        textMessage.dst = this.dstCallsign;
        textMessage.digipath = this.digipath;
        textMessage.text = this.textMessage;
        textMessage.ackId = this.ackId;
        return textMessage;
    }

    @Override
    public void fromBinary(String srcCallsign, String dstCallsign, String digipath, byte[] infoData) {
        _isValid = false;
        if (infoData.length < 10) return;
        this.digipath = digipath;
        this.srcCallsign = srcCallsign;
        ByteBuffer buffer = ByteBuffer.wrap(infoData);

        // callsign, trim ending spaces
        byte[] callsign = new byte[9];
        buffer.get(callsign);
        this.dstCallsign = new String(callsign).replaceAll("\\s+$", "");

        // ':' separator
        byte b = buffer.get();
        if (b != ':') return;

        // message
        byte[] message = new byte[buffer.remaining()];
        buffer.get(message);
        String stringMessage = new String(message, StandardCharsets.UTF_8);

        // ack/rej message
        this.ackId = null;
        Pattern p = Pattern.compile("^(ack|rej)([A-Za-z0-9]{1,5})[}]?$", Pattern.DOTALL);
        Matcher m = p.matcher(stringMessage);
        if (m.find()) {
            String type = m.group(1);
            if (type != null) {
                this.textMessage = m.group(1);
                String ackIdStr = m.group(2);
                if (ackIdStr != null) {
                    this.ackId = ackIdStr;
                }
            }
        } else {
            // message requires acknowledge {xxxxx (for auto ack)
            p = Pattern.compile("^(.+)[{]([A-Za-z0-9]{1,5})[}]?$", Pattern.DOTALL);
            m = p.matcher(stringMessage);
            if (m.find()) {
                this.textMessage = m.group(1);
                String ackNumStr = m.group(2);
                if (ackNumStr != null)
                    this.ackId = ackNumStr;
            } else {
                this.textMessage = stringMessage;
            }
        }

        // TODO, telemetry, make subclass from message, extend and extract values
        if (this.textMessage != null)
            _isValid = !isTelemetry(this.textMessage);
    }

    @Override
    public byte[] toBinary() {
        return (ackId != null)
                ? String.format(Locale.US, ":%-9s:%s{%s", dstCallsign, textMessage, ackId).getBytes(StandardCharsets.UTF_8)
                : String.format(":%-9s:%s", dstCallsign, textMessage).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean isValid() {
        return _isValid;
    }

    private boolean isTelemetry(String textMessage) {
        Pattern p = Pattern.compile("^(EQNS|PARM|UNIT|BITS)[.].+$", Pattern.DOTALL);
        Matcher m = p.matcher(textMessage);
        return m.matches();
    }
}
