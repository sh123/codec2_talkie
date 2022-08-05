package com.radio.codec2talkie.rigctl;

import android.content.Context;

import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;

public class Ft817 implements RigCtl {

    @Override
    public void initialize(Transport transport, Context context, RigCtlCallback protocolCallback) throws IOException {
    }

    @Override
    public void pttOn() {
    }

    @Override
    public void pttOff() {
    }
}
