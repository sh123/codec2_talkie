package com.radio.codec2talkie.connect;

import com.hoho.android.usbserial.driver.UsbSerialPort;

public class UsbPortHandler {
    private static UsbSerialPort port;
    private static String name;

    public static synchronized UsbSerialPort getPort(){
        return port;
    }
    public static synchronized String getName(){
        return name;
    }

    public static synchronized void setPort(UsbSerialPort port){
        UsbPortHandler.port = port;
    }
    public static synchronized void setName(String name){
        UsbPortHandler.name = name;
    }
}
