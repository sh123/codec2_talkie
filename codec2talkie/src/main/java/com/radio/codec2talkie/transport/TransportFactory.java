package com.radio.codec2talkie.transport;

import com.radio.codec2talkie.connect.SocketHandler;
import com.radio.codec2talkie.connect.UsbPortHandler;

import java.io.IOException;

public class TransportFactory {

    public enum TransportType {
        USB,
        BLUETOOTH,
        LOOPBACK
    };

    public static Transport create(TransportType transportType) throws IOException {
        switch (transportType) {
            case USB:
                return new UsbSerial(UsbPortHandler.getPort());
            case BLUETOOTH:
                return new Bluetooth(SocketHandler.getSocket());
            case LOOPBACK:
            default:
                return new Loopback();
        }
    }
}
