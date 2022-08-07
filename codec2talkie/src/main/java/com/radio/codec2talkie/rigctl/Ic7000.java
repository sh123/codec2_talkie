package com.radio.codec2talkie.rigctl;

public class Ic7000 extends Icom {
    @Override
    protected byte getCivAddress() {
        return (byte)0x70;
    }
}