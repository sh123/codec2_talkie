package com.radio.codec2talkie.connect;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class BleGattWrapper extends BluetoothGattCallback {

    private static final String TAG = BleGattWrapper.class.getSimpleName();

    public static final UUID BT_KISS_SERVICE_UUID = UUID.fromString("00000001-ba2a-46c9-ae49-01b0961f68bb");
    public static final UUID BT_KISS_CHARACTERISTIC_TX_UUID = UUID.fromString("00000002-ba2a-46c9-ae49-01b0961f68bb");
    public static final UUID BT_KISS_CHARACTERISTIC_RX_UUID = UUID.fromString("00000003-ba2a-46c9-ae49-01b0961f68bb");
    public static final UUID BT_NOTIFY_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothGatt _gatt;
    private BluetoothGattCharacteristic _rxCharacteristic;
    private BluetoothGattCharacteristic _txCharacteristic;

    private final Context _context;
    private final Handler _callback;

    private final ByteBuffer _readBuffer;
    private final ByteBuffer _writeBuffer;

    private boolean _isConnected;

    public BleGattWrapper(Context context, Handler callback) {
        _context = context;
        _callback = callback;

        int BUFFER_SIZE = 1024;
        _readBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        _writeBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

        _isConnected = false;
    }

    public void connect(BluetoothDevice device) {
        _gatt = device.connectGatt(_context, true, this);
    }

    public void close() {
        _gatt.close();
    }

    public int read(byte[] data) throws IOException {
        if (!_isConnected) throw new IOException();

        synchronized (_readBuffer) {
            // nothing to read
            if (_readBuffer.position() == 0) return 0;

            _readBuffer.flip();

            int countRead = 0;
            try {
                for (int i = 0; i < data.length; i++) {
                    byte b = _readBuffer.get();
                    data[i] = b;
                    countRead++;
                }
                _readBuffer.compact();
            } catch (BufferUnderflowException ignored) {
                _readBuffer.clear();
            }
            // enable for READ characteristic
            //_gatt.readCharacteristic(_rxCharacteristic);
            return countRead;
        }
    }

    public int write(byte[] data) throws IOException {
        if (!_isConnected) throw new IOException();

        synchronized (_writeBuffer) {
            _writeBuffer.put(data);
            _writeBuffer.flip();

            byte[] arr = new byte[_writeBuffer.limit()];
            _writeBuffer.get(arr);
            _txCharacteristic.setValue(arr);

            if (_gatt.writeCharacteristic(_txCharacteristic)) {
                // written successfully
                _writeBuffer.clear();
            } else {
                // redo
                _writeBuffer.position(_writeBuffer.limit());
                _writeBuffer.limit(_writeBuffer.capacity());
            }
            return data.length;
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        if (status == BluetoothGatt.GATT_SUCCESS && characteristic.getUuid().compareTo(BT_KISS_CHARACTERISTIC_RX_UUID) == 0) {
            synchronized (_readBuffer) {
                _readBuffer.put(characteristic.getValue());
            }
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        if (characteristic.getUuid().compareTo(BT_KISS_CHARACTERISTIC_RX_UUID) == 0) {
            synchronized (_readBuffer) {
                _readBuffer.put(characteristic.getValue());
            }
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);

        if (status == BluetoothGatt.GATT_SUCCESS && _writeBuffer.position() > 0) {
            synchronized (_writeBuffer) {
                _writeBuffer.flip();

                byte[] arr = new byte[_writeBuffer.limit()];
                _writeBuffer.get(arr);
                characteristic.setValue(arr);

                _gatt.writeCharacteristic(characteristic);
                _writeBuffer.clear();
            }
        }
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        Message resultMsg = Message.obtain();
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            resultMsg.what = BleConnectActivity.BT_GATT_CONNECT_SUCCESS;
            gatt.discoverServices();
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            _isConnected = false;
            resultMsg.what = BleConnectActivity.BT_GATT_CONNECT_FAILURE;
        }
        _callback.sendMessage(resultMsg);
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        Message resultMsg = Message.obtain();
        if (status == BluetoothGatt.GATT_SUCCESS) {
            resultMsg.what = BleConnectActivity.BT_SERVICES_DISCOVERED;
            if (initializeCharacteristics()) {
                _isConnected = true;
            } else {
                resultMsg.what = BleConnectActivity.BT_UNSUPPORTED_CHARACTERISTICS;
            }
        } else {
            resultMsg.what = BleConnectActivity.BT_SERVICES_DISCOVER_FAILURE;
        }
        _callback.sendMessage(resultMsg);
    }

    private boolean initializeCharacteristics() {
        for (BluetoothGattService service : _gatt.getServices()) {

            // ignore unknown services
            if (service.getUuid().compareTo(BT_KISS_SERVICE_UUID) != 0) continue;

            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                int properties = characteristic.getProperties();

                // RX characteristic
                if (characteristic.getUuid().compareTo(BT_KISS_CHARACTERISTIC_RX_UUID) == 0 &&
                        ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0)) {
                    _rxCharacteristic = characteristic;

                    // enable notifications
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BT_NOTIFY_UUID);
                    if (descriptor == null) {
                        Log.e(TAG, "Failed to get notification descriptor for " + characteristic.getUuid());
                    } else {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        _gatt.writeDescriptor(descriptor);
                    }
                    _gatt.setCharacteristicNotification(_rxCharacteristic, true);

                // TX characteristic
                } else if (characteristic.getUuid().compareTo(BT_KISS_CHARACTERISTIC_TX_UUID) == 0 &&
                        ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0)) {
                    _txCharacteristic = characteristic;
                }
            }
        }
        return (_rxCharacteristic != null && _txCharacteristic != null);
    }
}
