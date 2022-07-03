package com.radio.codec2talkie.tracker;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.radio.codec2talkie.protocol.position.Position;
import com.radio.codec2talkie.settings.PreferenceKeys;
import com.radio.codec2talkie.tools.UnitTools;

import java.util.Timer;
import java.util.TimerTask;

public class Manual implements Tracker {
    private boolean _isTracking = false;

    private TrackerCallback _trackerCallback;

    private Timer _sendTimer;

    private double _latitude;
    private double _longitude;
    private int _updateIntervalMinutes;
    private boolean _autoSendEnabled;

    @Override
    public void initialize(Context context, TrackerCallback trackerCallback) {
        _trackerCallback  = trackerCallback;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        _latitude = Double.parseDouble(sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_SOURCE_MANUAL_LAT, "0.0"));
        _longitude = Double.parseDouble(sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_SOURCE_MANUAL_LON, "0.0"));
        _updateIntervalMinutes = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.APRS_LOCATION_SOURCE_MANUAL_UPDATE_INTERVAL_MINUTES, "5"));
        _autoSendEnabled = sharedPreferences.getBoolean(PreferenceKeys.APRS_LOCATION_SOURCE_MANUAL_AUTO_SEND, true);
    }

    @Override
    public void sendPosition() {
        Position position = new Position();
        position.latitude = _latitude;
        position.longitude = _longitude;
        _trackerCallback.onSendLocation(position);
    }

    @Override
    public void startTracking() {
        sendPosition();
        _sendTimer = new Timer();
        _sendTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (_autoSendEnabled)
                    sendPosition();
                startTracking();
            }
        }, UnitTools.minutesToMillis(_updateIntervalMinutes));
        _isTracking = true;
    }

    @Override
    public void stopTracking() {
        if (_sendTimer != null) {
            _sendTimer.cancel();
            _sendTimer.purge();
            _sendTimer = null;
        }
        _isTracking = false;
    }

    @Override
    public boolean isTracking() {
        return _isTracking;
    }
}
