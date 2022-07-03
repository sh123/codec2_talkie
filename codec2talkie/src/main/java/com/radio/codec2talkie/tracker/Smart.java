package com.radio.codec2talkie.tracker;

import android.content.Context;
import android.location.LocationManager;

public class Smart implements Tracker {
    private boolean _isTracking = false;
    private TrackerCallback _trackerCallback;

    @Override
    public void initialize(Context context, TrackerCallback trackerCallback) {
        _trackerCallback  = trackerCallback;
    }

    @Override
    public void sendPosition() {
    }

    @Override
    public void startTracking() {
        _isTracking = true;
    }

    @Override
    public void stopTracking() {
        _isTracking = true;
    }

    @Override
    public boolean isTracking() {
        return _isTracking;
    }
}
