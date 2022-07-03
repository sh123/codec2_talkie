package com.radio.codec2talkie.tracker;

import android.content.Context;

public interface Tracker {
    void initialize(Context context, TrackerCallback trackerCallback);
    void sendPosition();
    void startTracking();
    void stopTracking();
    boolean isTracking();
}
