package com.radio.codec2talkie.connect;

import android.bluetooth.BluetoothSocket;

public class BluetoothSocketHandler {
    private static BluetoothSocket socket;
    private static String name;

    public static synchronized BluetoothSocket getSocket(){
        return socket;
    }
    public static synchronized String getName(){
        return name;
    }

    public static synchronized void setSocket(BluetoothSocket socket){
        BluetoothSocketHandler.socket = socket;
    }
    public static synchronized void setName(String name){
        BluetoothSocketHandler.name = name;
    }
}
