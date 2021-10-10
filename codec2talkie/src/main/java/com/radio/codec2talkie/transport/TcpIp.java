package com.radio.codec2talkie.transport;

import java.io.IOException;
import java.net.Socket;

public class TcpIp implements Transport {

    private final Socket _socket;

    public TcpIp(Socket socket) throws IOException {
        _socket = socket;
    }

    @Override
    public int read(byte[] data) throws IOException {
        return _socket.getInputStream().read(data);
    }

    @Override
    public int write(byte[] data) throws IOException {
        _socket.getOutputStream().write(data);
        return data.length;
    }

    @Override
    public void close() throws IOException {
        _socket.close();
    }
}
