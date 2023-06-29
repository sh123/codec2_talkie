package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.BuildConfig;
import com.radio.codec2talkie.R;
import com.radio.codec2talkie.protocol.aprs.AprsCallsign;
import com.radio.codec2talkie.protocol.aprs.tools.AprsIsData;
import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.tools.DebugTools;
import com.radio.codec2talkie.tools.TextTools;
import com.radio.codec2talkie.transport.TcpIp;
import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import kotlin.text.MatchGroup;
import kotlin.text.MatchResult;
import kotlin.text.Regex;

public class AprsIs implements Protocol, Runnable {
    private static final String TAG = AprsIs.class.getSimpleName();

    private static final int APRSIS_RETRY_WAIT_MS = 10000;
    private static final int APRSIS_DEFAULT_PORT = 14580;

    private final Protocol _childProtocol;
    private Context _context;
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
    private final byte[] _rxBuf;

    protected boolean _isRunning = true;
    private boolean _isConnected = false;

    public AprsIs(Protocol childProtocol) {
        _childProtocol = childProtocol;
        _rxQueue = ByteBuffer.allocate(4096);
        _txQueue = ByteBuffer.allocate(4096);
        _rxBuf = new byte[4096];
    }

    @Override
    public void initialize(Transport transport, Context context, ProtocolCallback protocolCallback) throws IOException {
        _parentProtocolCallback = protocolCallback;
        _context = context;
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
    public void sendData(String src, String dst, String path, byte[] data) throws IOException {
        if (_isSelfEnabled) {
            AprsIsData aprsIsData = new AprsIsData(src, dst, path, new String(data));
            synchronized (_txQueue) {
                _txQueue.put(aprsIsData.toString().getBytes());
            }
        }
        _childProtocol.sendData(src, dst, path, data);
    }

    @Override
    public boolean receive() throws IOException {
        String line;
        synchronized (_rxQueue) {
            line = TextTools.getString(_rxQueue);
        }
        if (line.length() > 0) {
            Log.d(TAG, "APRS-RX: " + DebugTools.bytesToDebugString(line.getBytes()));
            AprsIsData aprsIsData = AprsIsData.fromString(line);
            if (aprsIsData != null) {
                _parentProtocolCallback.onReceiveData(aprsIsData.src, aprsIsData.dst, aprsIsData.rawDigipath, aprsIsData.data.getBytes());
                if (_isTxGateEnabled && new AprsCallsign(aprsIsData.src).isValid) {
                    _childProtocol.sendData(aprsIsData.src, aprsIsData.dst, aprsIsData.digipath, aprsIsData.data.getBytes());
                }
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
                AprsIsData aprsIsData = new AprsIsData(src, dst, path, new String(data));
                synchronized (_txQueue) {
                    _txQueue.put(aprsIsData.toString().getBytes());
                }
            }
            _parentProtocolCallback.onReceiveData(src, dst, path, data);
        }

        @Override
        protected void onReceiveSignalLevel(short rssi, short snr) {
            _parentProtocolCallback.onReceiveSignalLevel(rssi, snr);
        }

        @Override
        protected void onReceiveTelemetry(int batVoltage) {
            _parentProtocolCallback.onReceiveTelemetry(batVoltage);
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

    @Override
    public void run() {
        Looper.prepare();
        TcpIp tcpIp = null;
        Log.i(TAG, "Started APRS-IS thread");
        while (_isRunning) {
            // connect
            if (!_isConnected) {
                tcpIp = runConnect();
            }
            if (tcpIp == null) {
                _isConnected = false;
                continue;
            }
            runRead(tcpIp);
            runWrite(tcpIp);
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

    private String getLoginCommand() {
        String cmd = "user " + _callsign + "-" + _ssid + " pass " + _passcode + " vers " + "C2T " + BuildConfig.VERSION_NAME;
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

    private TcpIp runConnect() {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(_server, APRSIS_DEFAULT_PORT));
            TcpIp tcpIp = new TcpIp(socket, "aprsis");
            String loginCmd = getLoginCommand();
            Log.i(TAG, "Login command " + loginCmd);
            tcpIp.write(loginCmd.getBytes());
            Log.i(TAG, "Connected to " + _server);
            Toast.makeText(_context, _context.getString(R.string.aprsis_connected), Toast.LENGTH_LONG).show();
            _isConnected = true;
            return tcpIp;
        } catch (IOException e) {
            Log.w(TAG, "Failed to connect");
            Toast.makeText(_context, _context.getString(R.string.aprsis_connect_failed), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            try {
                Thread.sleep(APRSIS_RETRY_WAIT_MS);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
            _isConnected = false;
            return null;
        }
    }

    private void runWrite(TcpIp tcpIp) {
        synchronized (_txQueue) {
            String line = TextTools.getString(_txQueue);
            if (line.length() > 0) {
                Log.d(TAG, "APRS-IS TX: " + DebugTools.bytesToDebugString(line.getBytes()));
                try {
                    tcpIp.write(line.getBytes());
                } catch (IOException e) {
                    Log.w(TAG, "Lost connection on transmit");
                    Toast.makeText(_context, _context.getString(R.string.aprsis_disconnected), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                    _isConnected = false;
                }
            }
        }
    }

    private void runRead(TcpIp tcpIp) {
        // read data
        int bytesRead;
        try {
            // # aprsc 2.1.11-g80df3b4 20 Aug 2022 11:33:40 GMT T2FINLAND 85.188.1.129:14580
            // # logresp N0CALL unverified, server T2GYOR<0xd><0xa>
            bytesRead = tcpIp.read(_rxBuf);
        } catch (IOException e) {
            Log.w(TAG, "Lost connection on receive");
            Toast.makeText(_context, _context.getString(R.string.aprsis_disconnected), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            _isConnected = false;
            return;
        }
        if (bytesRead > 0) {
            // server message
            if (_rxBuf[0] == '#') {
                String srvMsg = new String(Arrays.copyOf(_rxBuf, bytesRead));
                Log.d(TAG, "APRSIS: " + srvMsg);
                // wrong password
                if (srvMsg.matches("# logresp .+ unverified")) {
                    Toast.makeText(_context, _context.getString(R.string.aprsis_wrong_pass), Toast.LENGTH_LONG).show();
                    try {
                        tcpIp.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    _isConnected = false;
                }
                // update status
                Regex statusRegex = new Regex(".+ (\\S+ \\d{1,3}[.]\\d{1,3}[.]\\d{1,3}[.]\\d{1,3}:\\d+)");
                MatchResult matchResult = statusRegex.find(srvMsg, 0);
                if (matchResult != null) {
                    MatchGroup matchGroup = matchResult.getGroups().get(1);
                    if (matchGroup != null) {
                        Toast.makeText(_context, matchGroup.getValue(), Toast.LENGTH_SHORT).show();
                    }
                }
            // data
            } else {
                synchronized (_rxQueue) {
                    try {
                        _rxQueue.put(Arrays.copyOf(_rxBuf, bytesRead));
                    } catch (BufferOverflowException e) {
                        e.printStackTrace();
                        _rxQueue.clear();
                    }
                }
            }
        }
    }
}
