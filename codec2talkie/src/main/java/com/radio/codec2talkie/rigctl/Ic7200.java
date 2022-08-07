package com.radio.codec2talkie.rigctl;

public class Ic7200 extends Icom {
    @Override
    protected byte getCivAddress() {
        return (byte)0x76;
    }
}
