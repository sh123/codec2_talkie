package com.radio.codec2talkie.app;

public enum AppMessage {
    // events
    EV_DISCONNECTED(0),
    EV_CONNECTED(1),
    EV_LISTENING(2),
    EV_TRANSMITTED_VOICE(3),
    EV_TRANSMITTED_DATA(4),
    EV_RECEIVING(5),
    EV_VOICE_RECEIVED(6),
    EV_DATA_RECEIVED(7),
    EV_RX_LEVEL(8),
    EV_TX_LEVEL(9),
    EV_RX_ERROR(10),
    EV_TX_ERROR(11),
    EV_RX_RADIO_LEVEL(12),
    EV_STARTED_TRACKING(13),
    EV_STOPPED_TRACKING(14),
    // commands
    CMD_SEND_LOCATION_TO_TNC(15),
    CMD_PROCESS(16),
    CMD_QUIT(17),
    CMD_START_TRACKING(18),
    CMD_STOP_TRACKING(19),
    CMD_SEND_SINGLE_TRACKING(20);

    private final int _value;

    AppMessage(int value) {
        _value = value;
    }

    public int toInt() {
        return _value;
    }
}