package com.radio.codec2talkie.app;

public enum AppMessage {
    // events
    EV_DISCONNECTED(0),
    EV_CONNECTED(1),
    EV_LISTENING(2),
    EV_TRANSMITTED_VOICE(3),
    EV_TEXT_MESSAGE_TRANSMITTED(4),
    EV_TRANSMITTED_DATA(5),
    EV_RECEIVING(6),
    EV_VOICE_RECEIVED(7),
    EV_TEXT_MESSAGE_RECEIVED(8),
    EV_DATA_RECEIVED(9),
    EV_RX_LEVEL(10),
    EV_TX_LEVEL(11),
    EV_RX_ERROR(12),
    EV_TX_ERROR(13),
    EV_RX_RADIO_LEVEL(14),
    EV_STARTED_TRACKING(15),
    EV_STOPPED_TRACKING(16),
    // commands
    CMD_SEND_LOCATION_TO_TNC(17),
    CMD_PROCESS(18),
    CMD_QUIT(19),
    CMD_START_TRACKING(20),
    CMD_STOP_TRACKING(21),
    CMD_SEND_SINGLE_TRACKING(22),
    CMD_SEND_MESSAGE(23);

    private final int _value;

    AppMessage(int value) {
        _value = value;
    }

    public int toInt() {
        return _value;
    }
}