package com.radio.codec2talkie.rigctl;

import android.content.Context;

import com.radio.codec2talkie.protocol.ProtocolCallback;
import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;

public interface RigCtl {
    void initialize(Transport transport, Context context, RigCtlCallback protocolCallback) throws IOException;

    void pttOn();
    void pttOff();
}
