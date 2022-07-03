package com.radio.codec2talkie.tracker;

import com.radio.codec2talkie.protocol.position.Position;

public interface TrackerCallback {
    void onSendLocation(Position position);
}
