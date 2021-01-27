package com.radio.codec2talkie.connect;

import com.hoho.android.usbserial.driver.UsbSerialPort;

public class UsbPortHandler {
    private static UsbSerialPort port;

    public static synchronized UsbSerialPort getPort(){
        return port;
    }

    public static synchronized void setPort(UsbSerialPort port){
        UsbPortHandler.port = port;
    }
}
