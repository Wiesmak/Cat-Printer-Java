package com.catprinter;

import com.catprinter.command.Crc8;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Crc8Test {

    @Test
    void emptyInput() {
        assertEquals(0x00, Crc8.compute(new byte[0]));
    }

    @Test
    void singleByteZero() {
        assertEquals(0x00, Crc8.compute(new byte[] {0}));
    }

    @Test
    void singleByteOne() {
        assertEquals(0x07, Crc8.compute(new byte[] {1}));
    }

    @Test
    void singleByteFf() {
        assertEquals(0xf3, Crc8.compute(new byte[] {(byte) 0xff}));
    }

    @Test
    void fourBytes() {
        assertEquals(0x48, Crc8.compute(new byte[] {0x00, 0x01, 0x02, 0x03}));
    }

    @Test
    void allBytes() {
        byte[] all = new byte[256];
        for (int i = 0; i < 256; i++) all[i] = (byte) i;
        assertEquals(0x14, Crc8.compute(all));
    }
}
