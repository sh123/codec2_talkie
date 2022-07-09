package com.radio.codec2talkie.app;

public enum AppMessage {
    // events
    EV_DISCONNECTED(0),
    EV_CONNECTED(1),
    EV_LISTENING(2),
    EV_TRANSMITTING(3),
    EV_RECEIVING(4),
    EV_PLAYING(5),
    EV_RX_LEVEL(6),
    EV_TX_LEVEL(7),
    EV_RX_ERROR(8),
    EV_TX_ERROR(9),
    EV_RX_RADIO_LEVEL(10),
    EV_STARTED_TRACKING(11),
    EV_STOPPED_TRACKING(12),
    // commands
    CMD_SEND_LOCATION_TO_TNC(13),
    CMD_PROCESS(14),
    CMD_QUIT(15),
    CMD_START_TRACKING(16),
    CMD_STOP_TRACKING(17),
    CMD_SEND_SINGLE_TRACKING(18);

    private final int _value;

    AppMessage(int value) {
        _value = value;
    }

    public int toInt() {
        return _value;
    }
};