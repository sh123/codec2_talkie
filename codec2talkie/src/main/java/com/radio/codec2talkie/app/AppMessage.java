package com.radio.codec2talkie.app;

public enum AppMessage {
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
    CMD_SEND_LOCATION(11),
    CMD_PROCESS(12),
    CMD_QUIT(13);

    private final int _value;

    AppMessage(int value) {
        _value = value;
    }

    public int toInt() {
        return _value;
    }
};