package com.radio.codec2talkie.connect;

import java.net.Socket;

public class TcpIpSocketHandler {
    private static Socket socket;

    public static synchronized Socket getSocket(){
        return socket;
    }

    public static synchronized void setSocket(Socket socket){
        TcpIpSocketHandler.socket = socket;
    }
}
