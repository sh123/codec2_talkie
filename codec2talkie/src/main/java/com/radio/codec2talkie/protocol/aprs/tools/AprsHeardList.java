package com.radio.codec2talkie.protocol.aprs.tools;

import java.util.Map;
import java.util.TreeMap;

public class AprsHeardList {

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
            cleanup();
        }
    }

    public boolean contains(String callsign) {
        synchronized (_data) {
            cleanup();
            return _data.containsKey(callsign);
        }
    }

    private void cleanup() {
        long removeOlderThan = System.currentTimeMillis() - _keepSeconds * 1000L;
        for (Map.Entry<String, AprsHeardListItem> entryElement : _data.entrySet()) {
            if (entryElement.getValue().timestamp < removeOlderThan) {
                _data.remove(entryElement.getKey());
            }
        }
    }
}
