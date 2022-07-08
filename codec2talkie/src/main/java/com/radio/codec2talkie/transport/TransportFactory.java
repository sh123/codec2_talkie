package com.radio.codec2talkie.transport;

import com.radio.codec2talkie.connect.BleHandler;
import com.radio.codec2talkie.connect.BluetoothSocketHandler;
import com.radio.codec2talkie.connect.TcpIpSocketHandler;
import com.radio.codec2talkie.connect.UsbPortHandler;

import java.io.IOException;

public class TransportFactory {

    public enum TransportType {
        USB,
        BLUETOOTH,
        LOOPBACK,
        TCP_IP,
        BLE
    };

    public static Transport create(TransportType transportType) throws IOException {
        switch (transportType) {
            case USB:
                return new UsbSerial(UsbPortHandler.getPort(), UsbPortHandler.getName());
            case BLUETOOTH:
                return new Bluetooth(BluetoothSocketHandler.getSocket(), BluetoothSocketHandler.getName());
            case TCP_IP:
                return new TcpIp(TcpIpSocketHandler.getSocket(), TcpIpSocketHandler.getName());
            case BLE:
                return new Ble(BleHandler.getGatt(), BleHandler.getName());
            case LOOPBACK:
            default:
                return new Loopback();
        }
    }
}
