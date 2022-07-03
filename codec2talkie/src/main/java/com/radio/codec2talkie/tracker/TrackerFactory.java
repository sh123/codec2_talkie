package com.radio.codec2talkie.tracker;

import androidx.annotation.NonNull;

public class TrackerFactory {

    public enum TrackerType {
        MANUAL("manual"),
        PERIODIC("periodic"),
        SMART("smart");

        private final String _name;

        TrackerType(String name) {
            _name = name;
        }

        @NonNull
        @Override
        public String toString() {
            return _name;
        }
    }

    public static Tracker create(String trackerName) {
        TrackerType trackerType = TrackerType.valueOf(trackerName.toUpperCase());
        switch (trackerType) {
            case PERIODIC:
                return new Periodic();
            case SMART:
                return new Smart();
            case MANUAL:
            default:
                return new Manual();
        }
    }
}
