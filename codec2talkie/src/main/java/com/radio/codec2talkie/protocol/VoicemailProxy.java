package com.radio.codec2talkie.protocol;

import android.content.Context;

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

public class VoicemailProxy implements Protocol {

    private final int ROTATION_DELAY_MS = 10000;

    Context _context;
    File _storage;
    FileOutputStream _activeStream;
    Timer _fileRotationTimer;

    final Protocol _protocol;
    final int _codec2ModeId;

    public VoicemailProxy(Protocol protocol, int codec2ModeId) {
        _protocol = protocol;
        _codec2ModeId = codec2ModeId;
    }

    @Override
    public void initialize(Transport transport, Context context) throws IOException {
        _context = context;
        _storage = StorageTools.getStorage(context);
        _protocol.initialize(transport, context);
    }

    @Override
    public void send(byte[] frame) throws IOException {
        _protocol.send(frame);
        writeToFile(frame);
    }

    @Override
    public boolean receive(Callback callback) throws IOException {
        return _protocol.receive(new Callback() {
            @Override
            protected void onReceiveAudioFrames(byte[] audioFrames) {
                callback.onReceiveAudioFrames(audioFrames);
                writeToFile(audioFrames);
            }

            @Override
            protected void onReceiveSignalLevel(byte[] rawData) {
                callback.onReceiveSignalLevel(rawData);
            }
        });
    }

    @Override
    public void flush() throws IOException {
        _protocol.flush();
    }

    private void writeToFile(byte[] rawData)  {
        stopRotationTimer();
        if (_activeStream == null) {
            try {
                _activeStream = new FileOutputStream(new File(_storage, getNewFileName()));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        try {
            _activeStream.write(rawData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        startRotationTimer();
    }

    private String getNewFileName() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);
        return formatter.format(new Date()) + "_" + _codec2ModeId + ".c2";
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
                try {
                    _activeStream = new FileOutputStream(new File(_storage, getNewFileName()));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
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
