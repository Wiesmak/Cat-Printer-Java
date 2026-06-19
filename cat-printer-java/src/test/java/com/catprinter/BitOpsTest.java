package com.catprinter;

import com.catprinter.command.BitOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BitOpsTest {

    @Test
    void reverseBitsKnownValues() {
        assertEquals(0x00, BitOps.reverseBits(0x00));
        assertEquals(0x80, BitOps.reverseBits(0x01));
        assertEquals(0x01, BitOps.reverseBits(0x80));
        assertEquals(0xff, BitOps.reverseBits(0xff));
        assertEquals(0xa5, BitOps.reverseBits(0xa5));
        assertEquals(0x5a, BitOps.reverseBits(0x5a));
        assertEquals(0xf0, BitOps.reverseBits(0x0f));
    }

    @Test
    void reverseBitsRoundTrips() {
        for (int i = 0; i < 256; i++) {
            assertEquals(i, BitOps.reverseBits(BitOps.reverseBits(i)));
        }
    }

    @Test
    void intToBytesLittleEndian() {
        assertArrayEquals(new byte[] {0x00}, BitOps.intToBytes(0));
        assertArrayEquals(new byte[] {0x01}, BitOps.intToBytes(1));
        assertArrayEquals(new byte[] {(byte) 0xff}, BitOps.intToBytes(0xff));
        assertArrayEquals(new byte[] {0x32}, BitOps.intToBytes(50));
        assertArrayEquals(new byte[] {0x00, 0x01}, BitOps.intToBytes(0x100, 2));
        assertArrayEquals(new byte[] {0x34, 0x12}, BitOps.intToBytes(0x1234, 2));
    }

    @Test
    void intToBytesBigEndian() {
        assertArrayEquals(new byte[] {0x12, 0x34}, BitOps.intToBytes(0x1234, 2, true));
    }

    @Test
    void intToBytesClampsOverflow() {
        assertArrayEquals(new byte[] {(byte) 0xff}, BitOps.intToBytes(0x100, 1));
    }
}
