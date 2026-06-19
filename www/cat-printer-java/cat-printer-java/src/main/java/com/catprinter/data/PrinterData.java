package com.catprinter.data;

import com.catprinter.command.BitOps;

import java.util.ArrayList;
import java.util.List;

public final class PrinterData {

    private final int width;
    private final int dataWidth;
    private final long maxSize;
    private final long maxHeight;
    private final byte[] buffer;
    private int writePos;
    private long height;
    private boolean full;
    private final List<Integer> pages = new ArrayList<>();

    public PrinterData(int width) {
        this(width, 64L * 1024L * 1024L);
    }

    public PrinterData(int width, long maxSize) {
        if (width % 8 != 0) {
            throw new IllegalArgumentException("width must be a multiple of 8: " + width);
        }
        if (maxSize > Integer.MAX_VALUE) {
            maxSize = Integer.MAX_VALUE;
        }
        this.width = width;
        this.dataWidth = width / 8;
        this.maxSize = maxSize;
        this.maxHeight = maxSize / dataWidth;
        this.buffer = new byte[(int) maxSize];
        this.writePos = 0;
        this.height = 0;
        this.full = false;
    }

    public int width() {
        return width;
    }

    public int dataWidth() {
        return dataWidth;
    }

    public long height() {
        return height;
    }

    public boolean isFull() {
        return full;
    }

    public List<Integer> pages() {
        return pages;
    }

    public void addPage(int pageHeight) {
        pages.add(pageHeight);
    }

    public byte[] rawBuffer() {
        return buffer;
    }

    public int rawSize() {
        return full ? buffer.length : writePos;
    }

    public int write(byte[] data, int offset, int length) {
        if (writePos + length > buffer.length) {
            full = true;
            writePos = 0;
        }
        System.arraycopy(data, offset, buffer, writePos, length);
        writePos += length;
        if (!full) {
            height = (long) writePos / dataWidth;
        }
        return writePos;
    }

    public int write(byte[] data) {
        return write(data, 0, data.length);
    }

    public byte[] readChunk(int offset, int length) {
        int available = rawSize() - offset;
        if (available <= 0) {
            return new byte[0];
        }
        int n = Math.min(length, available);
        byte[] out = new byte[n];
        System.arraycopy(buffer, offset, out, 0, n);
        return out;
    }

    public void flip(boolean horizontally, boolean vertically) {
        if (!horizontally && !vertically) {
            return;
        }
        int rows = (int) height;
        if (horizontally) {
            for (int row = 0; row < rows; row++) {
                int rowStart = row * dataWidth;
                for (int i = 0; i < dataWidth; i++) {
                    buffer[rowStart + i] = (byte) BitOps.reverseBits(buffer[rowStart + i]);
                }
                for (int i = 0, j = dataWidth - 1; i < j; i++, j--) {
                    byte tmp = buffer[rowStart + i];
                    buffer[rowStart + i] = buffer[rowStart + j];
                    buffer[rowStart + j] = tmp;
                }
            }
        }
        if (vertically) {
            for (int i = 0, j = rows - 1; i < j; i++, j--) {
                int top = i * dataWidth;
                int bot = j * dataWidth;
                for (int k = 0; k < dataWidth; k++) {
                    byte tmp = buffer[top + k];
                    buffer[top + k] = buffer[bot + k];
                    buffer[bot + k] = tmp;
                }
            }
        }
    }

    public byte[] toPbmMerged() {
        String header = "P4\n" + width + " " + height + "\n";
        byte[] head = header.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        int payload = (int) (height * dataWidth);
        byte[] out = new byte[head.length + payload];
        System.arraycopy(head, 0, out, 0, head.length);
        System.arraycopy(buffer, 0, out, head.length, payload);
        return out;
    }
}
