package com.radio.codec2talkie.rigctl;

public class Ic7300 extends Icom {
    @Override
    protected byte getCivAddress() {
        return (byte)0x94;
    }
}
