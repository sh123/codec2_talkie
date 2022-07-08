package com.radio.codec2talkie.connect;

import java.net.Socket;

public class TcpIpSocketHandler {
    private static Socket socket;
    private static String name;

    public static synchronized Socket getSocket(){
        return socket;
    }
    public static synchronized String getName() { return name; }

    public static synchronized void setSocket(Socket socket){
        TcpIpSocketHandler.socket = socket;
    }
    public static synchronized void setName(String name){
        TcpIpSocketHandler.name = name;
    }
}
