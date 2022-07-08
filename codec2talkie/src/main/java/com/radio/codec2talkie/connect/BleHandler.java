package com.radio.codec2talkie.connect;

public class BleHandler {
    private static BleGattWrapper bleGatt;
    private static String name;

    public static synchronized BleGattWrapper getGatt(){
        return bleGatt;
    }
    public static synchronized String getName(){
        return name;
    }

    public static synchronized void setGatt(BleGattWrapper gatt){
        BleHandler.bleGatt = gatt;
    }
    public static synchronized void setName(String name){
        BleHandler.name = name;
    }
}
