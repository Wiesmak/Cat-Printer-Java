package com.catprinter;

import com.catprinter.data.PbmReader;
import com.catprinter.data.PrinterData;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrinterDataTest {

    @Test
    void readsPbm() throws IOException {
        byte[] header = "P4\n16 4\n".getBytes();
        byte[] payload = new byte[16 / 8 * 4];
        for (int i = 0; i < payload.length; i++) payload[i] = (byte) i;
        byte[] file = new byte[header.length + payload.length];
        System.arraycopy(header, 0, file, 0, header.length);
        System.arraycopy(payload, 0, file, header.length, payload.length);

        PrinterData data = new PrinterData(16);
        PbmReader.read(new ByteArrayInputStream(file), data);

        assertEquals(4, data.height());
        assertEquals(1, data.pages().size());
        assertEquals(4, (int) data.pages().get(0));
    }

    @Test
    void rejectsWrongWidth() {
        byte[] file = "P4\n8 4\n\0\0\0\0".getBytes();
        PrinterData data = new PrinterData(16);
        assertThrows(IOException.class, () -> PbmReader.read(new ByteArrayInputStream(file), data));
    }

    @Test
    void flipVertically() {
        PrinterData data = new PrinterData(8);
        data.write(new byte[] {0x01});
        data.write(new byte[] {0x02});
        data.write(new byte[] {0x03});
        assertEquals(3, data.height());
        data.flip(false, true);
        assertEquals(0x03, data.rawBuffer()[0] & 0xff);
        assertEquals(0x02, data.rawBuffer()[1] & 0xff);
        assertEquals(0x01, data.rawBuffer()[2] & 0xff);
    }

    @Test
    void flipHorizontallyReversesBitsAndBytes() {
        PrinterData data = new PrinterData(16);
        // Two bytes per row, one row.
        data.write(new byte[] {0x01, (byte) 0x80});
        data.flip(true, false);
        // After horizontal flip: each byte's bits reversed AND bytes swapped.
        // 0x01 -> 0x80, 0x80 -> 0x01, then swap -> [0x01, 0x80]
        assertEquals(0x01, data.rawBuffer()[0] & 0xff);
        assertEquals(0x80, data.rawBuffer()[1] & 0xff);
    }

    @Test
    void roundTripToPbm() throws IOException {
        byte[] header = "P4\n8 2\n".getBytes();
        byte[] payload = {0x12, 0x34};
        byte[] file = new byte[header.length + payload.length];
        System.arraycopy(header, 0, file, 0, header.length);
        System.arraycopy(payload, 0, file, header.length, payload.length);

        PrinterData data = new PrinterData(8);
        PbmReader.read(new ByteArrayInputStream(file), data);
        byte[] out = data.toPbmMerged();
        // Verify header reconstitutes
        String s = new String(out, 0, header.length);
        assertEquals("P4\n8 2\n", s);
        assertEquals(0x12, out[header.length] & 0xff);
        assertEquals(0x34, out[header.length + 1] & 0xff);
    }
}
