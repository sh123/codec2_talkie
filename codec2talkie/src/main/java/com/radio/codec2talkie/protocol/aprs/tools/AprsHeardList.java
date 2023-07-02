package com.radio.codec2talkie.protocol.aprs.tools;

import java.util.Map;

import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

public class AprsHeardList {

    private final int CLEANUP_PERIOD_MS = 30000;

    private static class AprsHeardListItem {
        public long timestamp;
        public String callsign;

        public AprsHeardListItem(long timestamp, String callsign) {
            this.timestamp = timestamp;
            this.callsign = callsign;
        }
    }

    private final int _keepSeconds;
    private final TreeMap<String, AprsHeardListItem> _data = new TreeMap<>();

    public AprsHeardList(int keepSeconds) {
        _keepSeconds = keepSeconds;
        scheduleCleanup();
    }

    public void add(String callsign) {
        synchronized (_data) {
            AprsHeardListItem heardItem = _data.get(callsign);
            if (heardItem == null) {
                AprsHeardListItem newHeardItem = new AprsHeardListItem(System.currentTimeMillis(), callsign);
                _data.put(callsign, newHeardItem);
            } else {
                heardItem.timestamp = System.currentTimeMillis();
            }
        }
    }

    public boolean contains(String callsign) {
        synchronized (_data) {
            return _data.containsKey(callsign);
        }
    }

    private void scheduleCleanup() {
        Timer cleanupTimer = new Timer();
        cleanupTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                cleanup();
            }
        }, CLEANUP_PERIOD_MS, CLEANUP_PERIOD_MS);
    }

    private void cleanup() {
        long removeOlderThan = System.currentTimeMillis() - _keepSeconds * 1000L;
        synchronized (_data) {
            for (Map.Entry<String, AprsHeardListItem> entryElement : _data.entrySet()) {
                if (entryElement.getValue().timestamp < removeOlderThan) {
                    _data.remove(entryElement.getKey());
                }
            }
        }
    }
}
