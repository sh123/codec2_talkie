package com.radio.codec2talkie.connect;

import android.bluetooth.BluetoothGatt;

public class BleHandler {
    private static BluetoothGatt bluetoothGatt;

    public static synchronized BluetoothGatt getSocket(){
        return bluetoothGatt;
    }

    public static synchronized void setGatt(BluetoothGatt gatt){
        BleHandler.bluetoothGatt = gatt;
    }
}
