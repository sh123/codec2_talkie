package com.radio.codec2talkie.tools;

import java.nio.ByteBuffer;

public class BitTools {

    public static byte[] convertToNRZI(byte[] bitsAsBytes) {
        ByteBuffer buffer = ByteBuffer.allocate(bitsAsBytes.length);
        byte last = 0;
        for (byte bitAsByte : bitsAsBytes) {
            if (bitAsByte == 0) {
                last = last == 0 ? (byte) 1 : (byte) 0;
            }
            buffer.put(last);
        }
        buffer.flip();
        byte[] r = new byte[buffer.remaining()];
        buffer.get(r);
        return r;
    }

    public static byte[] convertToHDLCBitArray(byte[] data, boolean shouldBitStuff) {
        ByteBuffer bitBuffer = ByteBuffer.allocate(2 * data.length * 8);

        int bitStuffCnt = 5;
        int cntOnes = 0;
        for (int i = 0; i < 8 * data.length; i++) {
            int b = ((int) data[i / 8]) & 0xff;
            // HDLC transmits least significant bit first
            if ((b & (1 << (i % 8))) > 0) {
                bitBuffer.put((byte) 1);
                if (shouldBitStuff)
                    cntOnes += 1;
            } else {
                bitBuffer.put((byte) 0);
                cntOnes = 0;
            }
            if (cntOnes == bitStuffCnt) {
                bitBuffer.put((byte) 0);
                cntOnes = 0;
            }
        }
        bitBuffer.flip();
        byte[] r = new byte[bitBuffer.remaining()];
        bitBuffer.get(r);
        return r;
    }
}
