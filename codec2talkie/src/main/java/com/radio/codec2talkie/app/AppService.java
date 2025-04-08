package com.radio.codec2talkie.app;

import android.annotation.SuppressLint;
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
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.MainActivity;
import com.radio.codec2talkie.R;
import com.radio.codec2talkie.protocol.message.TextMessage;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.storage.message.MessageItemActivity;
import com.radio.codec2talkie.tracker.Tracker;
import com.radio.codec2talkie.tracker.TrackerFactory;
import com.radio.codec2talkie.transport.TransportFactory;

import java.io.IOException;
import java.util.Objects;

public class AppService extends Service {

    public static TransportFactory.TransportType transportType;
    public static boolean isRunning = false;

    private static final String TAG = AppService.class.getSimpleName();

    private final int SERVICE_NOTIFICATION_ID = 1;
    private final int VOICE_NOTIFICATION_ID = 2;
    private final int MESSAGE_NOTIFICATION_ID = 3;

    private AppWorker _appWorker;
    private Messenger _callbackMessenger;
    private Tracker _tracker;
    private NotificationManager _notificationManager;
    private PowerManager.WakeLock _serviceWakeLock;

    private boolean _voiceNotificationsEnabled = false;

    private final IBinder _binder  = new AppServiceBinder();

    public class AppServiceBinder extends Binder {
        public AppService getService() {
            return AppService.this;
        }
    }

    public String getTransportName() {
        if (_appWorker != null) {
            return _appWorker.getTransportName();
        } else {
            return "";
        }
    }

    public void startTransmit() {
        if (_appWorker != null)
            _appWorker.startTransmit();
    }

    public void startReceive() {
        if (_appWorker != null)
            _appWorker.startReceive();
    }

    public void sendPosition() {
        Message msg = Message.obtain();
        msg.what = AppMessage.CMD_SEND_SINGLE_TRACKING.toInt();
        _onProcess.sendMessage(msg);
    }

    public void startTracking() {
        Message msg = Message.obtain();
        msg.what = AppMessage.CMD_START_TRACKING.toInt();
        _onProcess.sendMessage(msg);
    }

    public void stopTracking() {
        Message msg = Message.obtain();
        msg.what = AppMessage.CMD_STOP_TRACKING.toInt();
        _onProcess.sendMessage(msg);
    }

    public void sendTextMessage(TextMessage textMessage) {
        if (_appWorker != null)
            _appWorker.sendTextMessage(textMessage);
    }

    public boolean isTracking() {
        if (_tracker == null) return false;
        return _tracker.isTracking();
    }

    public void stopRunning() {
        Log.i(TAG, "stopRunning()");
        if (_appWorker != null)
            _appWorker.stopRunning();
    }

    @Override
    public void onRebind(Intent intent) {
        Log.i(TAG, "onRebind()");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind()");
        _callbackMessenger = null;
        return true;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind()");
        return _binder;
    }

    @SuppressLint("WakelockTimeout")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand()");

        // update callback from new intent
        Bundle extras = intent.getExtras();
        assert extras != null;
        _callbackMessenger = (Messenger) extras.get("callback");

        // create if not running
        if (isRunning) {
            Log.i(TAG, "Not starting app service, already running");
        } else {
            _notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert _notificationManager != null;
            SharedPreferences _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

            _voiceNotificationsEnabled = _sharedPreferences.getBoolean(PreferenceKeys.APP_NOTIFICATIONS_VOICE, false);

            String trackerName = _sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_SOURCE, "manual");
            _tracker = TrackerFactory.create(trackerName);
            _tracker.initialize(getApplicationContext(), position -> { if (_appWorker != null) _appWorker.sendPositionToTnc(position); });

            Notification notification = buildServiceNotification(getString(R.string.app_service_notif_text_ptt_ready), R.drawable.ic_app_action);
            startForeground(SERVICE_NOTIFICATION_ID, notification);

            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);

            boolean noCpuSleep = _sharedPreferences.getBoolean(PreferenceKeys.APP_NO_CPU_SLEEP, false);
            if (noCpuSleep) {
                _serviceWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "App::Service");
                _serviceWakeLock.acquire();
            }

            isRunning = true;

            transportType = (TransportFactory.TransportType) extras.get("transportType");
            assert transportType != null;
            startAppWorker(transportType);

            Log.i(TAG, "App service started");
        }

        return START_REDELIVER_INTENT;
    }

    private void startAppWorker(TransportFactory.TransportType transportType) {

        try {
            Log.i(TAG, "Started app worker: " + transportType.toString());

            _appWorker = new AppWorker(transportType,
                    _onProcess,
                    getApplicationContext());
            _appWorker.start();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(AppService.this, R.string.worker_failed_to_start_processing, Toast.LENGTH_LONG).show();
        }
    }

    private void deliverToParent(Message msg) throws RemoteException {
        if (_callbackMessenger != null) {
            Message sendMsg = new Message();
            sendMsg.copyFrom(msg);
            _callbackMessenger.send(sendMsg);
        }
    }

    private final Handler _onProcess = new Handler(Objects.requireNonNull(Looper.myLooper())) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            try {
                deliverToParent(msg);

                switch (AppMessage.values()[msg.what]) {
                    case EV_DISCONNECTED:
                        isRunning = false;
                        _appWorker = null;
                        _tracker.stopTracking();
                        if (_serviceWakeLock != null) _serviceWakeLock.release();
                        showServiceNotification(R.string.app_service_notif_connection_lost, R.drawable.ic_app_action_disconnected);
                        break;
                    case EV_VOICE_RECEIVED:
                        showVoiceNotification(R.string.app_notifications_voice_title, R.string.app_notifications_voice_summary);
                        break;
                    case EV_TEXT_MESSAGE_RECEIVED:
                        showMessageNotification(R.string.app_notifications_text_title, (String)msg.obj);
                        break;
                    case EV_LISTENING:
                        hideVoiceNotification();
                        break;
                    case CMD_SEND_SINGLE_TRACKING:
                        _tracker.sendPosition();
                        break;
                    case CMD_START_TRACKING:
                        showServiceNotification(R.string.app_service_notif_text_tracking, R.drawable.ic_app_action);
                        _tracker.startTracking();
                        Message startedTrackingMessage = Message.obtain();
                        startedTrackingMessage.what = AppMessage.EV_STARTED_TRACKING.toInt();
                        deliverToParent(startedTrackingMessage);
                        break;
                    case CMD_STOP_TRACKING:
                        showServiceNotification(R.string.app_service_notif_text_ptt_ready, R.drawable.ic_app_action);
                        _tracker.stopTracking();
                        Message stoppedTrackingMessage = Message.obtain();
                        stoppedTrackingMessage.what = AppMessage.EV_STOPPED_TRACKING.toInt();
                        deliverToParent(stoppedTrackingMessage);
                        break;
                    default:
                        break;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };

    private void showServiceNotification(int resId, int iconResId) {
        String text = getString(resId);
        _notificationManager.notify(SERVICE_NOTIFICATION_ID, buildServiceNotification(text, iconResId));
    }

    private void showVoiceNotification(int titleResId, int textResId) {
        if (!MainActivity.isPaused || !_voiceNotificationsEnabled) return;
        String title = getString(titleResId);
        String text = getString(textResId);
        _notificationManager.notify(VOICE_NOTIFICATION_ID, buildFullScreenNotification(title, text));
    }

    private void showMessageNotification(int titleResId, String note) {
        if (!MessageItemActivity.isPaused || !_voiceNotificationsEnabled) return;
        String title = getString(titleResId);
        _notificationManager.notify(MESSAGE_NOTIFICATION_ID, buildMessageNotification(title, note));
    }

    private void hideVoiceNotification() {
        if (!_voiceNotificationsEnabled) return;
        _notificationManager.cancel(VOICE_NOTIFICATION_ID);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(String channelId, String channelName, int importance) {
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW);
        channel.setImportance(importance);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        _notificationManager.createNotificationChannel(channel);
        return channelId;
    }

    private Notification buildServiceNotification(String text, int iconResId) {
        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            channelId = createNotificationChannel("alpha", "Service", NotificationManager.IMPORTANCE_LOW);

        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, channelId)
                .setOngoing(true)
                .setSmallIcon(iconResId)
                .setContentTitle(getString(R.string.app_service_notif_title))
                .setContentText(text)
                .setPriority(NotificationManagerCompat.IMPORTANCE_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setChannelId(channelId)
                .setContentIntent(intent)
                .setOnlyAlertOnce(true)
                .build();
    }

    private Notification buildFullScreenNotification(String title, String text) {
        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            channelId = createNotificationChannel("beta", "Voice", NotificationManager.IMPORTANCE_HIGH);

        Intent fullScreenIntent = new Intent(this, MainActivity.class);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 0,
                fullScreenIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_app_action)
                .setChannelId(channelId)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setAutoCancel(true)
                .build();
    }

    private Notification buildMessageNotification(String title, String text) {
        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            channelId = createNotificationChannel("gamma", "Message", NotificationManager.IMPORTANCE_HIGH);

        // extract group name from the note
        String[] srcDstText = text.split(": ");
        String groupName = srcDstText[0].split("→")[1];

        Intent notificationIntent = new Intent(this, MessageItemActivity.class);
        notificationIntent.putExtra("groupName", groupName);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_app_action)
                .setChannelId(channelId)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
    }
}
