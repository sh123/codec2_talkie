package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.radio.codec2talkie.MainActivity;
import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.tools.StorageTools;
import com.radio.codec2talkie.transport.Transport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class Recorder implements Protocol {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int ROTATION_DELAY_MS = 5000;

    private File _storage;
    private FileOutputStream _activeStream;
    private Timer _fileRotationTimer;

    final Protocol _childProtocol;
    final int _codec2ModeId;

    private String _prevSrcCallsign;
    private String _prevDstCallsign;

    private ProtocolCallback _parentProtocolCallback;

    public Recorder(Protocol childProtocol, int codec2ModeId) {
        _childProtocol = childProtocol;
        _codec2ModeId = codec2ModeId;

        _prevSrcCallsign = null;
        _prevDstCallsign = null;
    }

    @Override
    public void initialize(Transport transport, Context context, ProtocolCallback protocolCallback) throws IOException {
        _parentProtocolCallback = protocolCallback;
        _storage = StorageTools.getStorage(context);
        _childProtocol.initialize(transport, context, _protocolCallback);
    }

    @Override
    public int getPcmAudioBufferSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendCompressedAudio(String src, String dst, int codec2Mode, byte[] frame) throws IOException {
        rotateIfNewSrcOrDstCallsign(src, dst);
        writeToFile(src, dst, codec2Mode, frame);
        _childProtocol.sendCompressedAudio(src, dst, codec2Mode, frame);
    }

    @Override
    public void sendTextMessage(TextMessage textMessage) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendPcmAudio(String src, String dst, int codec, short[] pcmFrame) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendData(String src, String dst, byte[] dataPacket) throws IOException {
        _childProtocol.sendData(src, dst, dataPacket);
    }

    @Override
    public boolean receive() throws IOException {
        return _childProtocol.receive();
    }

    ProtocolCallback _protocolCallback = new ProtocolCallback() {
        @Override
        protected void onReceivePosition(Position position) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void onReceivePcmAudio(String src, String dst, int codec, short[] pcmFrame) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void onReceiveCompressedAudio(String src, String dst, int codec2Mode, byte[] audioFrames) {
            rotateIfNewSrcOrDstCallsign(src, dst);
            _parentProtocolCallback.onReceiveCompressedAudio(src, dst, codec2Mode, audioFrames);
            writeToFile(src, dst, codec2Mode, audioFrames);
        }

        @Override
        protected void onReceiveTextMessage(TextMessage textMessage) {
            _parentProtocolCallback.onReceiveTextMessage(textMessage);
        }

        @Override
        protected void onReceiveData(String src, String dst, byte[] data) {
            _parentProtocolCallback.onReceiveData(src, dst, data);
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
            throw new UnsupportedOperationException();
        }

        @Override
        protected void onTransmitData(String src, String dst, byte[] data) {
            _parentProtocolCallback.onTransmitData(src, dst, data);
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
    public void sendPosition(Position position) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flush() throws IOException {
        _childProtocol.flush();
    }

    @Override
    public void close() {
        _childProtocol.close();
    }

    private void writeToFile(String src, String dst, int codec2Mode, byte[] rawData)  {
        stopRotationTimer();
        createStreamIfNotExists(src, dst, codec2Mode);
        writeToStream(rawData);
        startRotationTimer();
    }

    private void writeToStream(byte[] rawData) {
        try {
            if (_activeStream != null) {
                _activeStream.write(rawData);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createStreamIfNotExists(String src, String dst, int codec2Mode) {
        if (_activeStream == null) {
            try {
                Date date = new Date();
                File newDirectory = new File(_storage, getNewDirectoryName(date));
                if (!newDirectory.exists() && !newDirectory.mkdirs()) {
                    Log.e(TAG, "Failed to create directory for voicemails");
                }
                File newAudioFile = new File(newDirectory, getNewFileName(date, src, dst, codec2Mode));
                _activeStream = new FileOutputStream(newAudioFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private String getNewDirectoryName(Date date) {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd", Locale.US);
        return df.format(date);
    }

    private String getNewFileName(Date date, String src, String dst, int codec2Mode) {
        int mode = codec2Mode;
        if (mode == -1) {
            mode = _codec2ModeId;
        }
        SimpleDateFormat tf = new SimpleDateFormat("HHmmss", Locale.ENGLISH);
        String codec2mode = String.format(Locale.ENGLISH, "%02d", mode);
        String fileName = codec2mode + "_" + tf.format(date);
        if (src != null && dst != null) {
            fileName += "_" + src + "_" + dst;
        }
        fileName += ".c2";
        return fileName;
    }

    private void rotateIfNewSrcOrDstCallsign(String newSrcCallsign, String newDstCallsign) {
        if (!TextUtils.equals(_prevSrcCallsign, newSrcCallsign) || !TextUtils.equals(_prevDstCallsign, newDstCallsign)) {
            _prevSrcCallsign = newSrcCallsign;
            _prevDstCallsign = newDstCallsign;
            if (_activeStream != null) {
                try {
                    _activeStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                _activeStream = null;
            }
        }
    }

    private void startRotationTimer() {
        _fileRotationTimer = new Timer();
        _fileRotationTimer.schedule(new TimerTask() {
            @Override
            public void run() {
            if (_activeStream != null) {
                try {
                    _activeStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            _activeStream = null;
            }
        }, ROTATION_DELAY_MS);
    }

    private void stopRotationTimer() {
        try {
            if (_fileRotationTimer != null) {
                _fileRotationTimer.cancel();
                _fileRotationTimer.purge();
            }
        } catch (IllegalStateException ignored) {}
    }
}
