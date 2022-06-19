package com.radio.codec2talkie.connect;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;

public class BleGattWrapper {
    public BluetoothGatt gatt;
    public BluetoothGattCharacteristic rxCharacteristic;
    public BluetoothGattCharacteristic txCharacteristic;

    private final UUID _serviceUuid;

    public BleGattWrapper(BluetoothGatt srcGatt, UUID serviceUuid)
    {
        gatt = srcGatt;
        _serviceUuid = serviceUuid;
    }

    public Boolean Initialize() {
        for (BluetoothGattService service : gatt.getServices()) {
            if (service.getUuid().compareTo(_serviceUuid) != 0) continue;
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                int properties = characteristic.getProperties();
                if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                    rxCharacteristic = characteristic;
                }
                else if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
                    txCharacteristic = characteristic;
                }
            }
        }
        return (rxCharacteristic != null && txCharacteristic != null);
    }
}
