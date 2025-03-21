package com.radio.codec2talkie.rigctl;

import android.content.Context;
import com.radio.codec2talkie.tools.FlashLight;
import com.radio.codec2talkie.transport.Transport;

import java.io.IOException;

public class PhoneTorch implements RigCtl {

    private FlashLight _flashLight;

    @Override
    public void initialize(Transport transport, Context context, RigCtlCallback protocolCallback) throws IOException {
        _flashLight = new FlashLight(context);
    }

    @Override
    public void pttOn() {
        _flashLight.turnOn();
    }

    @Override
    public void pttOff() {
        _flashLight.turnOff();
    }
}
