package com.radio.codec2talkie.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class TcpIp implements Transport {

    private final Socket _socket;

    private final InputStream _inputStream;
    private final OutputStream _outputStream;

    public TcpIp(Socket socket) throws IOException {
        _socket = socket;
        _inputStream = _socket.getInputStream();
        _outputStream = _socket.getOutputStream();
    }

    @Override
    public int read(byte[] data) throws IOException {
        return _inputStream.read(data);
    }

    @Override
    public int write(byte[] data) throws IOException {
        _outputStream.write(data);
        return data.length;
    }

    @Override
    public void close() throws IOException {
        _socket.close();
    }
}
