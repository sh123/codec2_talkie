package com.radio.codec2talkie.rigctl;

public class X6100 extends Icom {
    @Override
    protected byte getCivAddress() { return (byte)0xa4; }
}