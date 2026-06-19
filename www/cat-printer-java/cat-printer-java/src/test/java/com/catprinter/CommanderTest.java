package com.catprinter;

import com.catprinter.command.Commander;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommanderTest {

    private static String capture(java.util.function.Consumer<Commander> action) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        Commander c = new Commander(b -> buf.write(b, 0, b.length));
        action.accept(c);
        return toHex(buf.toByteArray());
    }

    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    @Test
    void startPrinting() {
        assertEquals("5178a30001000000ff", capture(Commander::startPrinting));
    }

    @Test
    void startPrintingNew() {
        assertEquals("125178a30001000000ff", capture(Commander::startPrintingNew));
    }

    @Test
    void setDpiAs200() {
        assertEquals("5178a4000100329eff", capture(Commander::setDpiAs200));
    }

    @Test
    void setSpeed32() {
        assertEquals("5178bd00010020e0ff", capture(c -> c.setSpeed(32)));
    }

    @Test
    void setEnergy0x4000() {
        assertEquals("5178af0002000040c7ff", capture(c -> c.setEnergy(0x4000)));
    }

    @Test
    void feedPaper128() {
        assertEquals("5178a10002008000b6ff", capture(c -> c.feedPaper(128)));
    }

    @Test
    void startLattice() {
        assertEquals(
            "5178a6000b00aa551738445f5f5f44382ca1ff",
            capture(Commander::startLattice)
        );
    }

    @Test
    void endLattice() {
        assertEquals(
            "5178a6000b00aa5517000000000000001711ff",
            capture(Commander::endLattice)
        );
    }

    @Test
    void drawBitmap() {
        assertEquals(
            "5178a2000400804001ff51ff",
            capture(c -> c.drawBitmap(new byte[] {0x01, 0x02, (byte) 0x80, (byte) 0xff}))
        );
    }

    @Test
    void applyEnergy() {
        assertEquals("5178be0001000107ff", capture(Commander::applyEnergy));
    }

    @Test
    void updateDevice() {
        assertEquals("5178a90001000000ff", capture(Commander::updateDevice));
    }

    @Test
    void getDeviceState() {
        assertEquals("5178a30001000000ff", capture(Commander::getDeviceState));
    }
}
