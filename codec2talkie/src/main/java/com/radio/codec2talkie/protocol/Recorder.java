package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.radio.codec2talkie.MainActivity;
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

    private final int ROTATION_DELAY_MS = 5000;

    private File _storage;
    private FileOutputStream _activeStream;
    private Timer _fileRotationTimer;

    final Protocol _childProtocol;
    final int _codec2ModeId;

    private String _prevSrcCallsign;
    private String _prevDstCallsign;

    private Callback _parentCallback;

    public Recorder(Protocol childProtocol, int codec2ModeId) {
        _childProtocol = childProtocol;
        _codec2ModeId = codec2ModeId;

        _prevSrcCallsign = null;
        _prevDstCallsign = null;
    }

    @Override
    public void initialize(Transport transport, Context context, Callback callback) throws IOException {
        _parentCallback = callback;
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

    Callback _protocolCallback = new Callback() {
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
            _parentCallback.onReceiveCompressedAudio(src, dst, codec2Mode, audioFrames);
            writeToFile(src, dst, codec2Mode, audioFrames);
        }

        @Override
        protected void onReceiveData(String src, String dst, byte[] data) {
            _parentCallback.onReceiveData(src, dst, data);
        }

        @Override
        protected void onReceiveSignalLevel(short rssi, short snr) {
            _parentCallback.onReceiveSignalLevel(rssi, snr);
        }

        @Override
        protected void onReceiveLog(String logData) {
            _parentCallback.onReceiveLog(logData);
        }

        @Override
        protected void onTransmitPcmAudio(String src, String dst, int codec, short[] frame) {
            _parentCallback.onTransmitPcmAudio(src, dst, codec, frame);
        }

        @Override
        protected void onTransmitCompressedAudio(String src, String dst, int codec, byte[] frame) {
            _parentCallback.onTransmitCompressedAudio(src, dst, codec, frame);
        }

        @Override
        protected void onTransmitData(String src, String dst, byte[] data) {
            _parentCallback.onTransmitData(src, dst, data);
        }

        @Override
        protected void onTransmitLog(String logData) {
            _parentCallback.onTransmitLog(logData);
        }

        @Override
        protected void onProtocolRxError() {
            _parentCallback.onProtocolRxError();
        }

        @Override
        protected void onProtocolTxError() {
            _parentCallback.onProtocolTxError();
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
