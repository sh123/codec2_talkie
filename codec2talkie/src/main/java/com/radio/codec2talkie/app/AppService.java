package com.radio.codec2talkie.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.radio.codec2talkie.MainActivity;
import com.radio.codec2talkie.R;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.tracker.Tracker;
import com.radio.codec2talkie.tracker.TrackerFactory;
import com.radio.codec2talkie.transport.TransportFactory;

import java.io.IOException;

public class AppService extends Service {

    private static final String TAG = AppService.class.getSimpleName();

    private final int NOTIFICATION_ID = 1;

    private AppWorker _appWorker;
    private Messenger _callbackMessenger;
    private Tracker _tracker;
    private NotificationManager _notificationManager;

    private final IBinder _binder  = new AppServiceBinder();

    public class AppServiceBinder extends Binder {
        public AppService getService() {
            return AppService.this;
        }
    }

    public void startTransmit() {
        _appWorker.startTransmit();
    }

    public void startReceive() {
        _appWorker.startReceive();
    }

    public void sendPosition() {
        _tracker.sendPosition();
    }

    public void startTracking() {
        showNotification(R.string.app_service_notif_text_tracking);
        _tracker.startTracking();
    }

    public void stopTracking() {
        showNotification(R.string.app_service_notif_text_ptt_ready);
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
        Log.i(TAG, "Staring app service");

        _notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert _notificationManager != null;
        SharedPreferences _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Bundle extras = intent.getExtras();

        String trackerName = _sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_SOURCE, "manual");
        _tracker = TrackerFactory.create(trackerName);
        _tracker.initialize(getApplicationContext(), position -> _appWorker.sendPosition(position));

        TransportFactory.TransportType transportType = (TransportFactory.TransportType)extras.get("transportType");
        _callbackMessenger = (Messenger) extras.get("callback");
        startAppWorker(transportType);

        Notification notification = buildNotification(getString(R.string.app_service_notif_text_ptt_ready));
        startForeground(NOTIFICATION_ID, notification);

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

    private void showNotification(int resId) {
        String text = getString(resId);
        _notificationManager.notify(NOTIFICATION_ID, buildNotification(text));
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel() {
        String channelId = "alpha";
        String channelName = "Service";
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW);
        channel.setImportance(NotificationManager.IMPORTANCE_LOW);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        _notificationManager.createNotificationChannel(channel);
        return channelId;
    }

    private Notification buildNotification(String text) {
        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            channelId = createNotificationChannel();

        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), 0,
                notificationIntent, 0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
                this, channelId);
        return notificationBuilder
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_app_action)
                .setContentTitle(getString(R.string.app_service_notif_title))
                .setContentText(text)
                .setPriority(NotificationManagerCompat.IMPORTANCE_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setChannelId(channelId)
                .setContentIntent(intent)
                .setOnlyAlertOnce(true)
                .build();
    }
}
