package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.app.AppMessage;
import com.radio.codec2talkie.protocol.aprs.AprsData;
import com.radio.codec2talkie.protocol.aprs.AprsDataFactory;
import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.tools.DebugTools;
import com.radio.codec2talkie.tools.TextTools;
import com.radio.codec2talkie.transport.TcpIp;
import com.radio.codec2talkie.transport.Transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class AprsIs implements Protocol, Runnable {
    private static final String TAG = AprsIs.class.getSimpleName();

    private final Protocol _childProtocol;

    private ProtocolCallback _parentProtocolCallback;

    private String _passcode;
    private String _server;

    private boolean _isSelfEnabled;
    private boolean _isRxGateEnabled;
    private boolean _isTxGateEnabled;

    private String _callsign;
    private String _ssid;
    private int _filterRadius;
    private String _filter;

    private final ByteBuffer _rxQueue;
    private final ByteBuffer _txQueue;

    protected boolean _isRunning = true;

    public AprsIs(Protocol childProtocol) {
        _childProtocol = childProtocol;
        _rxQueue = ByteBuffer.allocate(4096);
        _txQueue = ByteBuffer.allocate(4096);
    }

    @Override
    public void initialize(Transport transport, Context context, ProtocolCallback protocolCallback) throws IOException {
        _parentProtocolCallback = protocolCallback;
        _childProtocol.initialize(transport, context, _protocolCallback);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        _isRxGateEnabled = sharedPreferences.getBoolean(PreferenceKeys.APRS_IS_ENABLE_RX_GATE, false);
        _isTxGateEnabled = sharedPreferences.getBoolean(PreferenceKeys.APRS_IS_ENABLE_TX_GATE, false);
        _isSelfEnabled = sharedPreferences.getBoolean(PreferenceKeys.APRS_IS_ENABLE_SELF, false);
        _callsign = sharedPreferences.getString(PreferenceKeys.AX25_CALLSIGN, "N0CALL");
        _ssid = sharedPreferences.getString(PreferenceKeys.AX25_SSID, "0");
        _passcode = sharedPreferences.getString(PreferenceKeys.APRS_IS_CODE, "");
        _server = sharedPreferences.getString(PreferenceKeys.APRS_IS_TCPIP_SERVER, "euro.aprs2.net");
        _filterRadius = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.APRS_IS_RADIUS, "10"));
        _filter = sharedPreferences.getString(PreferenceKeys.APRS_IS_FILTER, "");

        Log.i(TAG, "AprsIs RX gate: " + _isTxGateEnabled + ", TX gate: " + _isTxGateEnabled + ", server: " + _server);

        new Thread(this).start();
    }

    @Override
    public int getPcmAudioRecordBufferSize() {
        return _childProtocol.getPcmAudioRecordBufferSize();
    }

    @Override
    public void sendCompressedAudio(String src, String dst, int codec2Mode, byte[] frame) throws IOException {
        _childProtocol.sendCompressedAudio(src, dst, codec2Mode, frame);
    }

    @Override
    public void sendTextMessage(TextMessage textMessage) throws IOException {
        _childProtocol.sendTextMessage(textMessage);
    }

    @Override
    public void sendPcmAudio(String src, String dst, int codec2Mode, short[] pcmFrame) throws IOException {
        _childProtocol.sendPcmAudio(src, dst, codec2Mode, pcmFrame);
    }

    @Override
    public void sendData(String src, String dst, String path, byte[] dataPacket) throws IOException {
        if (_isSelfEnabled) {
            // TODO, forward own data to APRS-IS
        }
        _childProtocol.sendData(src, dst, path, dataPacket);
    }

    @Override
    public boolean receive() throws IOException {
        String line = "";
        synchronized (_rxQueue) {
            line = TextTools.getString(_rxQueue);
        }
        if (line.length() > 0) {
            if (_isTxGateEnabled) {
                // TODO, forward APRS-IS data to radio
            }
            AprsData aprsData = AprsDataFactory.fromAprsIs(line);
            if (aprsData != null && aprsData.isValid()) {
                // TODO, need to extract src, dst, digipath
                // _parentProtocolCallback.onReceiveData(aprsData.);
            }
            _parentProtocolCallback.onReceiveLog(line);
        }
        return _childProtocol.receive();
    }

    ProtocolCallback _protocolCallback = new ProtocolCallback() {
        @Override
        protected void onReceivePosition(Position position) {
            _parentProtocolCallback.onReceivePosition(position);
        }

        @Override
        protected void onReceivePcmAudio(String src, String dst, int codec, short[] pcmFrame) {
            _parentProtocolCallback.onReceivePcmAudio(src, dst, codec, pcmFrame);
        }

        @Override
        protected void onReceiveCompressedAudio(String src, String dst, int codec2Mode, byte[] audioFrame) {
            _parentProtocolCallback.onReceiveCompressedAudio(src, dst, codec2Mode, audioFrame);
        }

        @Override
        protected void onReceiveTextMessage(TextMessage textMessage) {
            _parentProtocolCallback.onReceiveTextMessage(textMessage);
        }

        @Override
        protected void onReceiveData(String src, String dst, String path, byte[] data) {
            if (_isRxGateEnabled) {
                // TODO, forward radio data to APRS-IS
            }
            _parentProtocolCallback.onReceiveData(src, dst, path, data);
        }

        @Override
        protected void onReceiveSignalLevel(short rssi, short snr) {
            _parentProtocolCallback.onReceiveSignalLevel(rssi, snr);
        }

        @Override
        protected void onReceiveLog(String logData) {
            _parentProtocolCallback.onReceiveLog(logData);
        }

        @Override
        protected void onTransmitPcmAudio(String src, String dst, int codec, short[] frame) {
            _parentProtocolCallback.onTransmitPcmAudio(src, dst, codec, frame);
        }

        @Override
        protected void onTransmitCompressedAudio(String src, String dst, int codec, byte[] frame) {
            _parentProtocolCallback.onTransmitCompressedAudio(src, dst, codec, frame);
        }

        @Override
        protected void onTransmitTextMessage(TextMessage textMessage) {
            _parentProtocolCallback.onTransmitTextMessage(textMessage);
        }

        @Override
        protected void onTransmitPosition(Position position) {
            _parentProtocolCallback.onTransmitPosition(position);
        }

        @Override
        protected void onTransmitData(String src, String dst, String path, byte[] data) {
            _parentProtocolCallback.onTransmitData(src, dst, path, data);
        }

        @Override
        protected void onTransmitLog(String logData) {
            _parentProtocolCallback.onTransmitLog(logData);
        }

        @Override
        protected void onProtocolRxError() {
            _parentProtocolCallback.onProtocolRxError();
        }

        @Override
        protected void onProtocolTxError() {
            _parentProtocolCallback.onProtocolTxError();
        }
    };

    @Override
    public void sendPosition(Position position) throws IOException {
        _childProtocol.sendPosition(position);
    }

    @Override
    public void flush() throws IOException {
        _childProtocol.flush();
    }

    @Override
    public void close() {
        Log.i(TAG, "close()");
        _isRunning = false;
        _childProtocol.close();
    }

    private String getLoginCommand() {
        String cmd = "user " + _callsign + "-" + _ssid + " pass " + _passcode + " vers " + "C2T 1.0";
        if (_filterRadius > 0) {
            cmd += " filter m/" + _filterRadius;
        }
        if (_filter.length() > 0) {
            if (!cmd.contains("filter")) {
                cmd += " filter ";
            }
            cmd += " " + _filter;
        }
        cmd += "\n";
        return cmd;
    }

    @Override
    public void run() {
        Socket socket;
        boolean isConnected = false;
        TcpIp tcpIp = null;
        byte[] buf = new byte[4096];

        Log.i(TAG, "Started APRS-IS thread");
        while (_isRunning) {
            // connect
            if (!isConnected) {
                socket = new Socket();
                try {
                    socket.connect(new InetSocketAddress(_server, 14580));
                    tcpIp = new TcpIp(socket, "aprsis");
                    String loginCmd = getLoginCommand();
                    Log.i(TAG, "Login command " + loginCmd);
                    tcpIp.write(loginCmd.getBytes());
                    Log.i(TAG, "Connected to " + _server);
                } catch (IOException e) {
                    Log.w(TAG, "Failed to connect");
                    e.printStackTrace();
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }
                    isConnected = false;
                    continue;
                }
                isConnected = true;
            }
            // read data
            int bytesRead = 0;
            try {
                // # aprsc 2.1.11-g80df3b4 20 Aug 2022 11:33:40 GMT T2FINLAND 85.188.1.129:14580
                // # logresp N0CALL unverified, server T2GYOR<0xd><0xa>
                bytesRead = tcpIp.read(buf);
            } catch (IOException e) {
                // TODO, notify parent
                Log.w(TAG, "Lost connection on receive");
                e.printStackTrace();
                isConnected = false;
                continue;
            }
            if (bytesRead > 0 && buf[0] != '#') {
                synchronized (_rxQueue) {
                    try {
                        _rxQueue.put(Arrays.copyOf(buf, bytesRead));
                    } catch (BufferOverflowException e) {
                        e.printStackTrace();
                        _rxQueue.clear();
                    }
                }
            }
            // write data
            synchronized (_txQueue) {
                String line = TextTools.getString(_txQueue);
                if (line.length() > 0) {
                    Log.v(TAG, line);
                    try {
                        tcpIp.write(line.getBytes());
                    } catch (IOException e) {
                        // TODO, notify parent
                        Log.w(TAG, "Lost connection on transmit");
                        e.printStackTrace();
                        isConnected = false;
                    }
                }
            }
        }

        if (tcpIp != null) {
            try {
                tcpIp.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Log.i(TAG, "Stopped APRS-IS thread");
    }
}
