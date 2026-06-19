package com.catprinter.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TextCanvas {

    private final Pf2Font font;
    private final int width;
    private final int height;
    private final boolean wrap;
    private final boolean rtl;
    private byte[] canvas;

    public TextCanvas(int width, Pf2Font font, boolean wrap, boolean rtl) {
        if (font.isBroken()) {
            throw new IllegalArgumentException("PF2 font is broken");
        }
        if (width % 8 != 0) {
            throw new IllegalArgumentException("width must be a multiple of 8: " + width);
        }
        this.font = font;
        this.width = width;
        this.wrap = wrap;
        this.rtl = rtl;
        this.height = font.maxHeight() + font.descent();
        this.canvas = new byte[width * height / 8];
    }

    public int width() { return width; }
    public int height() { return height; }
    public Pf2Font font() { return font; }

    public byte[] flushCanvas() {
        byte[] previous = canvas;
        canvas = new byte[width * height / 8];
        return previous;
    }

    public List<byte[]> putText(String text) {
        text = text.replace("\t", "    ");
        List<byte[]> lines = new ArrayList<>();
        int canvasLength = canvas.length;

        Map<Integer, Pf2Font.Character> characters = new HashMap<>();
        int currentWidth = 0;
        int lastSpaceAt = -1;
        int widthAtLastSpace = 0;
        Set<Integer> breakPoints = new HashSet<>();

        for (int i = 0; i < text.length(); i++) {
            int s = text.codePointAt(i);
            characters.computeIfAbsent(s, font::getChar);
            if (s == ' ') {
                lastSpaceAt = i;
                widthAtLastSpace = currentWidth;
            }
            if (currentWidth > width && lastSpaceAt != -1) {
                breakPoints.add(lastSpaceAt);
                currentWidth -= widthAtLastSpace;
            }
            if (s == '\n') {
                currentWidth = 0;
                continue;
            }
            currentWidth += font.pointSize() / 2;
        }

        currentWidth = 0;
        for (int i = 0; i < text.length(); i++) {
            int s = text.codePointAt(i);
            Pf2Font.Character ch = characters.get(s);
            if (ch == null) {
                continue;
            }
            boolean shouldBreak =
                (wrap && breakPoints.contains(i)) ||
                s == '\n' ||
                currentWidth + ch.width() > width;
            if (shouldBreak) {
                lines.add(flushCanvas());
                currentWidth = 0;
                if (s == ' ' || s == '\n') {
                    continue;
                }
            }
            if (s >= 0x00 && s < 0x20) {
                continue;
            }
            for (int x = 0; x < ch.width(); x++) {
                for (int y = 0; y < ch.height(); y++) {
                    int rtlCurrentWidth = width - currentWidth - ch.width() - 1;
                    int targetX = x + ch.xOffset();
                    int targetY = font.ascent() + (y - ch.height()) - ch.yOffset();
                    int xBase = rtl ? rtlCurrentWidth : currentWidth;
                    int bitIndex = width * targetY + xBase + targetX;
                    int canvasByte = bitIndex / 8;
                    int canvasBit = 7 - (bitIndex % 8);
                    if (canvasByte < 0 || canvasByte >= canvasLength) {
                        continue;
                    }
                    int bit = ch.getBit(x, y);
                    canvas[canvasByte] |= (byte) ((bit & 0x1) << canvasBit);
                }
            }
            currentWidth += ch.deviceWidth();
        }
        return lines;
    }

    public byte[] currentCanvas() {
        return canvas;
    }
}
