package com.radio.codec2talkie.tools;

import android.annotation.SuppressLint;

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

    public static byte[] convertFromNRZI(byte[] bitsAsBytes, byte prevBit) {
        ByteBuffer buffer = ByteBuffer.allocate(bitsAsBytes.length);
        byte last = prevBit;
        for (byte bitAsByte : bitsAsBytes) {
            // no transition -> 1
            if (last == bitAsByte) {
                buffer.put((byte) 1);
            // transition -> 0
            } else {
                buffer.put((byte) 0);
            }
            last = bitAsByte;
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
        // return
        bitBuffer.flip();
        byte[] r = new byte[bitBuffer.remaining()];
        bitBuffer.get(r);
        return r;
    }

    @SuppressLint("DefaultLocale")
    public static byte[] convertFromHDLCBitArray(byte[] dataBitsAsBytes) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(dataBitsAsBytes.length / 8);

        int currentByte = 0;
        int cntOnes = 0;
        int bitStuffCnt = 5;
        boolean skipNext = false;
        StringBuilder s = new StringBuilder();
        int cntBits = 0;
        for (byte currentBit : dataBitsAsBytes) {
            if (skipNext) {
                // cannot have 6 consecutive 1, non-HDLC data
                if (currentBit == 1) return null;
                s.append(String.format("[%d]", currentBit));
                skipNext = false;
                continue;
            }
            currentByte >>= 1;
            s.append(String.format("%d", currentBit));
            if (currentBit == 1) {
                currentByte |= (1 << 7);
                cntOnes++;
            } else {
                cntOnes = 0;
            }
            if (cntBits % 8 == 3) {
                s.append(':');
            }
            if (cntBits % 8 == 7) {
                s.append(' ');
                byteBuffer.put((byte) (currentByte & 0xff));
                currentByte = 0;
            }
            // 5 ones, skip next
            if (cntOnes == bitStuffCnt) {
                skipNext = true;
                cntOnes = 0;
            }
            cntBits++;
        }
        //if (cntBits % 8 != 0) return null;
        //Log.i("----", s.toString());
        // return
        byteBuffer.flip();
        byte[] r = new byte[byteBuffer.remaining()];
        byteBuffer.get(r);
        return r;
    }
}
