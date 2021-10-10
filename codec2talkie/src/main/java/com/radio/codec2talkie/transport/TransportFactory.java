package com.radio.codec2talkie.transport;

import com.radio.codec2talkie.connect.BluetoothSocketHandler;
import com.radio.codec2talkie.connect.TcpIpSocketHandler;
import com.radio.codec2talkie.connect.UsbPortHandler;

import java.io.IOException;

public class TransportFactory {

    public enum TransportType {
        USB,
        BLUETOOTH,
        LOOPBACK,
        TCP_IP
    };

    public static Transport create(TransportType transportType) throws IOException {
        switch (transportType) {
            case USB:
                return new UsbSerial(UsbPortHandler.getPort());
            case BLUETOOTH:
                return new Bluetooth(BluetoothSocketHandler.getSocket());
            case TCP_IP:
                return new TcpIp(TcpIpSocketHandler.getSocket());
            case LOOPBACK:
            default:
                return new Loopback();
        }
    }
}
