package com.radio.codec2talkie.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.R;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.tracker.Tracker;
import com.radio.codec2talkie.tracker.TrackerFactory;
import com.radio.codec2talkie.transport.TransportFactory;

import java.io.IOException;

public class AppService extends Service {

    private static final String TAG = AppService.class.getSimpleName();

    private AppWorker _appWorker;
    private Messenger _callbackMessenger;
    private Tracker _tracker;

    private final IBinder _binder  = new AppServiceBinder();

    public class AppServiceBinder extends Binder {
        public AppService getService() {
            return AppService.this;
        }
    }

    public void startRecording() {
        _appWorker.startRecording();
    }

    public void startPlayback() {
        _appWorker.startPlayback();
    }

    public void sendPosition() {
        _tracker.sendPosition();
    }

    public void startTracking() {
        _tracker.startTracking();
    }

    public void stopTracking() {
        _tracker.stopTracking();
    }

    public boolean isTracking() {
        return _tracker.isTracking();
    }

    public void stopRunning() {
        if (_appWorker != null) {
            _appWorker.stopRunning();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return _binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Bundle extras = intent.getExtras();

        String trackerName = _sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_SOURCE, "manual");
        _tracker = TrackerFactory.create(trackerName);
        _tracker.initialize(getApplicationContext(), position -> _appWorker.sendPosition(position));

        TransportFactory.TransportType transportType = (TransportFactory.TransportType)extras.get("transportType");
        _callbackMessenger = (Messenger) extras.get("callback");
        startAppWorker(transportType);

        showNotification();
        return START_STICKY;
    }

    private void startAppWorker(TransportFactory.TransportType transportType) {

        try {
            Log.i(TAG, "Started app worker: " + transportType.toString());

            _appWorker = new AppWorker(transportType,
                    onAudioProcessorStateChanged,
                    getApplicationContext());
            _appWorker.start();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(AppService.this, R.string.worker_failed_to_start_processing, Toast.LENGTH_LONG).show();
        }
    }

    private final Handler onAudioProcessorStateChanged = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            try {
                Message sendMsg = new Message();
                sendMsg.copyFrom(msg);
                _callbackMessenger.send(sendMsg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(NotificationManager notificationManager){
        String channelId = "alpha";
        String channelName = "codec2talkie";
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        channel.setImportance(NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationManager.createNotificationChannel(channel);
        return channelId;
    }

    private void showNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;

        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            channelId = createNotificationChannel(notificationManager);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
                this, channelId);
        Notification notification = notificationBuilder
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_app_action)
                .setContentTitle("Ready for PTT")
                .setContentText("Touch me to open rig")
                .setPriority(NotificationManagerCompat.IMPORTANCE_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setChannelId(channelId)
                .build();

        startForeground(1, notification);
    }
}
