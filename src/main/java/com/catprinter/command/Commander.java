package com.catprinter.command;

import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;

public class Commander {

    public static final String TX_CHARACTERISTIC = "0000ae01-0000-1000-8000-00805f9b34fb";
    public static final String RX_CHARACTERISTIC = "0000ae02-0000-1000-8000-00805f9b34fb";

    public static final byte[] DATA_FLOW_PAUSE = {
        (byte) 0x51, (byte) 0x78, (byte) 0xae, (byte) 0x01, (byte) 0x01,
        (byte) 0x00, (byte) 0x10, (byte) 0x70, (byte) 0xff
    };

    public static final byte[] DATA_FLOW_RESUME = {
        (byte) 0x51, (byte) 0x78, (byte) 0xae, (byte) 0x01, (byte) 0x01,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff
    };

    private final Consumer<byte[]> sink;

    public Commander(Consumer<byte[]> sink) {
        this.sink = sink;
    }

    protected void send(byte[] data) {
        sink.accept(data);
    }

    public byte[] makeCommand(int commandBit, byte[] payload) {
        if (payload.length > 0xff) {
            throw new IllegalArgumentException(
                "Command payload too big (" + payload.length + " > 255)"
            );
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(8 + payload.length);
        out.write(0x51);
        out.write(0x78);
        out.write(commandBit & 0xff);
        out.write(0x00);
        out.write(payload.length & 0xff);
        out.write(0x00);
        out.write(payload, 0, payload.length);
        out.write(Crc8.compute(payload) & 0xff);
        out.write(0xff);
        return out.toByteArray();
    }

    public void startPrinting() {
        send(new byte[] {
            (byte) 0x51, (byte) 0x78, (byte) 0xa3, (byte) 0x00, (byte) 0x01,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff
        });
    }

    public void startPrintingNew() {
        send(new byte[] {
            (byte) 0x12, (byte) 0x51, (byte) 0x78, (byte) 0xa3, (byte) 0x00,
            (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff
        });
    }

    public void applyEnergy() {
        send(makeCommand(0xbe, BitOps.intToBytes(0x01)));
    }

    public void getDeviceState() {
        send(makeCommand(0xa3, BitOps.intToBytes(0x00)));
    }

    public void getDeviceInfo() {
        send(makeCommand(0xa8, BitOps.intToBytes(0x00)));
    }

    public void updateDevice() {
        send(makeCommand(0xa9, BitOps.intToBytes(0x00)));
    }

    public void setDpiAs200() {
        send(makeCommand(0xa4, BitOps.intToBytes(50)));
    }

    public void startLattice() {
        send(makeCommand(0xa6, new byte[] {
            (byte) 0xaa, (byte) 0x55, (byte) 0x17, (byte) 0x38, (byte) 0x44,
            (byte) 0x5f, (byte) 0x5f, (byte) 0x5f, (byte) 0x44, (byte) 0x38,
            (byte) 0x2c
        }));
    }

    public void endLattice() {
        send(makeCommand(0xa6, new byte[] {
            (byte) 0xaa, (byte) 0x55, (byte) 0x17, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x17
        }));
    }

    public void retractPaper(int pixels) {
        send(makeCommand(0xa0, BitOps.intToBytes(pixels, 2)));
    }

    public void feedPaper(int pixels) {
        send(makeCommand(0xa1, BitOps.intToBytes(pixels, 2)));
    }

    public void setSpeed(int value) {
        send(makeCommand(0xbd, BitOps.intToBytes(value)));
    }

    public void setEnergy(int amount) {
        send(makeCommand(0xaf, BitOps.intToBytes(amount, 2)));
    }

    public void drawBitmap(byte[] bitmapData) {
        send(makeCommand(0xa2, BitOps.reverseBits(bitmapData)));
    }

    public void drawCompressedBitmap(byte[] bitmapData) {
        drawBitmap(bitmapData);
    }
}
