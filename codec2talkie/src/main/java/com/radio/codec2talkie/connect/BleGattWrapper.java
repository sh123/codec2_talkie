package com.radio.codec2talkie.connect;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.util.UUID;

public class BleGattWrapper extends BluetoothGattCallback {

    public BluetoothGatt gatt;
    public BluetoothGattCharacteristic rxCharacteristic;
    public BluetoothGattCharacteristic txCharacteristic;

    private final Context _context;
    private final UUID _serviceUuid;
    private final Handler _callback;

    public BleGattWrapper(Context context, UUID serviceUuid, Handler callback) {
        _serviceUuid = serviceUuid;
        _context = context;
        _callback = callback;
    }

    public void connect(BluetoothDevice device) {
        gatt = device.connectGatt(_context, true, this);
    }

    public void close() {
        gatt.close();
    }

    private boolean initializeCharacteristics() {
        for (BluetoothGattService service : gatt.getServices()) {
            if (service.getUuid().compareTo(_serviceUuid) != 0) continue;
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                int properties = characteristic.getProperties();
                if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                    rxCharacteristic = characteristic;
                } else if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
                    txCharacteristic = characteristic;
                }
            }
        }
        return (rxCharacteristic != null && txCharacteristic != null);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        Message resultMsg = Message.obtain();
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            resultMsg.what = BleConnectActivity.BT_GATT_CONNECT_SUCCESS;
            gatt.discoverServices();
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            resultMsg.what = BleConnectActivity.BT_GATT_CONNECT_FAILURE;
        }
        _callback.sendMessage(resultMsg);
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        Message resultMsg = Message.obtain();
        if (status == BluetoothGatt.GATT_SUCCESS) {
            resultMsg.what = BleConnectActivity.BT_SERVICES_DISCOVERED;
            if (!initializeCharacteristics()) {
                resultMsg.what = BleConnectActivity.BT_UNSUPPORTED_CHARACTERISTICS;
            }
        } else {
            resultMsg.what = BleConnectActivity.BT_SERVICES_DISCOVER_FAILURE;
        }
        _callback.sendMessage(resultMsg);
    }
}
