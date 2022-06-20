package com.radio.codec2talkie.connect;

public class BleHandler {
    private static BleGattWrapper bleGatt;

    public static synchronized BleGattWrapper getGatt(){
        return bleGatt;
    }

    public static synchronized void setGatt(BleGattWrapper gatt){
        BleHandler.bleGatt = gatt;
    }
}
