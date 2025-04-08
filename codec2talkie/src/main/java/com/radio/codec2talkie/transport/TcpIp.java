package com.radio.codec2talkie.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class TcpIp implements Transport {

    private final int RX_TIMEOUT = 10;

    private final Socket _socket;
    private final String _name;

    private final InputStream _inputStream;
    private final OutputStream _outputStream;

    public TcpIp(Socket socket, String name) throws IOException {
        _socket = socket;
        _socket.setSoTimeout(RX_TIMEOUT);
        _inputStream = _socket.getInputStream();
        _outputStream = _socket.getOutputStream();
        _name = name;
    }

    @Override
    public String name() {
        return _name;
    }

    @Override
    public int read(byte[] data) throws IOException {
        try {
            int bytesRead = _inputStream.read(data);
            // connection closed
            if (bytesRead == -1) {
                throw new IOException();
            }
            return bytesRead;
        } catch (SocketTimeoutException e) {
            return 0;
        }
    }

    @Override
    public int write(byte[] data) throws IOException {
        _outputStream.write(data);
        return data.length;
    }

    @Override
    public int read(short[] data) throws IOException {
        return 0;
    }

    @Override
    public int write(short[] data) throws IOException {
        return 0;
    }

    @Override
    public void close() throws IOException {
        _socket.close();
    }
}
