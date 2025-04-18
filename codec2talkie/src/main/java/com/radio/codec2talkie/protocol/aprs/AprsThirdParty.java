package com.radio.codec2talkie.protocol.aprs;

import com.radio.codec2talkie.protocol.aprs.tools.AprsIsData;
import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;

import java.nio.charset.StandardCharsets;

public class AprsThirdParty implements AprsData {

    private AprsData _aprsData;

    @Override
    public boolean isPositionReport() {
        return _aprsData.isPositionReport();
    }

    @Override
    public boolean isTextMessage() {
        return _aprsData.isTextMessage();
    }

    @Override
    public void fromPosition(Position position) {
        _aprsData.fromPosition(position);
    }

    @Override
    public void fromTextMessage(TextMessage textMessage) {
        _aprsData.fromTextMessage(textMessage);
    }

    @Override
    public Position toPosition() {
        return _aprsData.toPosition();
    }

    @Override
    public TextMessage toTextMessage() {
        return _aprsData.toTextMessage();
    }

    @Override
    public void fromBinary(String srcCallsign, String dstCallsign, String digipath, byte[] infoData) {
        AprsIsData data = AprsIsData.fromString(new String(infoData));
        if (data == null) return;
        AprsDataType aprsDataType = new AprsDataType(data.data.charAt(0));
        _aprsData = AprsDataFactory.create(aprsDataType);
        if (_aprsData == null) return;
        _aprsData.fromBinary(data.src, data.dst, data.rawDigipath, data.data.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public byte[] toBinary() {
        return _aprsData.toBinary();
    }

    @Override
    public boolean isValid() {
        if (_aprsData == null) return false;
        return _aprsData.isValid();
    }
}
