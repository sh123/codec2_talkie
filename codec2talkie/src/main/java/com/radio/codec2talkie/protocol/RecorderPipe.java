package com.radio.codec2talkie.protocol;

import android.content.Context;
import android.util.Log;

import com.radio.codec2talkie.MainActivity;
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

public class RecorderPipe implements Protocol {

    private static final String TAG = MainActivity.class.getSimpleName();

    private final int ROTATION_DELAY_MS = 10000;

    Context _context;
    File _storage;
    FileOutputStream _activeStream;
    Timer _fileRotationTimer;

    final Protocol _childProtocol;
    final int _codec2ModeId;

    public RecorderPipe(Protocol childProtocol, int codec2ModeId) {
        _childProtocol = childProtocol;
        _codec2ModeId = codec2ModeId;
    }

    @Override
    public void initialize(Transport transport, Context context) throws IOException {
        _context = context;
        _storage = StorageTools.getStorage(context);
        _childProtocol.initialize(transport, context);
    }

    @Override
    public void send(byte[] frame) throws IOException {
        _childProtocol.send(frame);
        writeToFile(frame);
    }

    @Override
    public boolean receive(Callback callback) throws IOException {
        return _childProtocol.receive(new Callback() {
            @Override
            protected void onReceiveAudioFrames(byte[] audioFrames) {
                callback.onReceiveAudioFrames(audioFrames);
                writeToFile(audioFrames);
            }

            @Override
            protected void onReceiveSignalLevel(byte[] rawData) {
                callback.onReceiveSignalLevel(rawData);
            }

            @Override
            protected void onProtocolError() {
                callback.onProtocolError();
            }
        });
    }

    @Override
    public void flush() throws IOException {
        _childProtocol.flush();
    }

    private void writeToFile(byte[] rawData)  {
        stopRotationTimer();
        createStreamIfNotExists();
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

    private void createStreamIfNotExists() {
        if (_activeStream == null) {
            try {
                Date date = new Date();
                File newDirectory = new File(_storage, getNewDirectoryName(date));
                if (!newDirectory.exists() && !newDirectory.mkdirs()) {
                    Log.e(TAG, "Failed to create directory for voicemails");
                }
                File newAudioFile = new File(newDirectory, getNewFileName(date));
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

    private String getNewFileName(Date date) {
        SimpleDateFormat tf = new SimpleDateFormat("HHmmss", Locale.ENGLISH);
        String codec2mode = String.format(Locale.ENGLISH, "%02d", _codec2ModeId);
        return codec2mode + "_" + tf.format(date)  + ".c2";
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
