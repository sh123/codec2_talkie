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

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class BleGattWrapper extends BluetoothGattCallback {

    private final int BUFFER_SIZE = 1024;

    private BluetoothGatt _gatt;
    private BluetoothGattCharacteristic _rxCharacteristic;
    private BluetoothGattCharacteristic _txCharacteristic;

    private final Context _context;
    private final UUID _serviceUuid;
    private final Handler _callback;

    private final ByteBuffer _readBuffer;

    public BleGattWrapper(Context context, UUID serviceUuid, Handler callback) {
        _serviceUuid = serviceUuid;
        _context = context;
        _callback = callback;
        _readBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    }

    public void connect(BluetoothDevice device) {
        _gatt = device.connectGatt(_context, true, this);
    }

    public void close() {
        _gatt.close();
    }

    public int read(byte[] data) throws IOException {
        int countRead = 0;
        try {
            for (int i = 0; i < data.length; i++) {
                byte b = _readBuffer.get();
                data[i] = b;
                countRead++;
            }
        } catch (BufferUnderflowException ignored) {
        }
        _gatt.readCharacteristic(_rxCharacteristic);
        return countRead;
    }

    public int write(byte[] data) throws IOException {
        _rxCharacteristic.setValue(data);
        _gatt.writeCharacteristic(_txCharacteristic);
        return data.length;
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        byte[] data = characteristic.getValue();
        _readBuffer.put(characteristic.getValue());
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        _readBuffer.put(characteristic.getValue());
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
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

    private boolean initializeCharacteristics() {
        for (BluetoothGattService service : _gatt.getServices()) {
            if (service.getUuid().compareTo(_serviceUuid) != 0) continue;
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                int properties = characteristic.getProperties();
                if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                    _rxCharacteristic = characteristic;
                    _gatt.setCharacteristicNotification(characteristic, true);
                } else if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
                    _txCharacteristic = characteristic;
                }
            }
        }
        return (_rxCharacteristic != null && _txCharacteristic != null);
    }
}
