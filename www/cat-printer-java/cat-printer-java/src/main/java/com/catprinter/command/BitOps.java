package com.catprinter.command;

public final class BitOps {

    private BitOps() {}

    public static int reverseBits(int b) {
        int i = b & 0xff;
        i = ((i & 0b10101010) >>> 1) | ((i & 0b01010101) << 1);
        i = ((i & 0b11001100) >>> 2) | ((i & 0b00110011) << 2);
        return ((i & 0b11110000) >>> 4) | ((i & 0b00001111) << 4);
    }

    public static byte[] reverseBits(byte[] data) {
        byte[] out = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            out[i] = (byte) reverseBits(data[i]);
        }
        return out;
    }

    public static byte[] intToBytes(int value, int length, boolean bigEndian) {
        if (value < 0) {
            throw new IllegalArgumentException("intToBytes: " + value + " < 0");
        }
        long maxValue = (1L << (length * 8L)) - 1L;
        long v = value;
        if (v > maxValue) {
            v = maxValue;
        }
        byte[] out = new byte[length];
        for (int p = 0; p < length && v != 0; p++) {
            out[p] = (byte) (v & 0xff);
            v >>>= 8;
        }
        if (bigEndian) {
            for (int i = 0, j = out.length - 1; i < j; i++, j--) {
                byte tmp = out[i];
                out[i] = out[j];
                out[j] = tmp;
            }
        }
        return out;
    }

    public static byte[] intToBytes(int value, int length) {
        return intToBytes(value, length, false);
    }

    public static byte[] intToBytes(int value) {
        return intToBytes(value, 1, false);
    }
}
