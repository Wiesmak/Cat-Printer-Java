package com.catprinter.text;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Pf2Font {

    public static class Character {
        int width;
        int height;
        int xOffset;
        int yOffset;
        int deviceWidth;
        byte[] bitmapData;

        public int width() { return width; }
        public int height() { return height; }
        public int xOffset() { return xOffset; }
        public int yOffset() { return yOffset; }
        public int deviceWidth() { return deviceWidth; }

        public int getBit(int x, int y) {
            int bitIndex = width * y + x;
            int byteIndex = bitIndex >>> 3;
            int bitPos = 7 - (bitIndex & 7);
            return (bitmapData[byteIndex] & (0x1 << bitPos)) >>> bitPos;
        }
    }

    static final class ScaledCharacter extends Character {
        int scale = 1;

        @Override
        public int getBit(int x, int y) {
            int sx = x / scale;
            int sy = y / scale;
            int unscaledWidth = width / scale;
            int bitIndex = unscaledWidth * sy + sx;
            int byteIndex = bitIndex >>> 3;
            int bitPos = 7 - (bitIndex & 7);
            return (bitmapData[byteIndex] & (0x1 << bitPos)) >>> bitPos;
        }
    }

    private static final byte[] PF2_MAGIC = {
        'F', 'I', 'L', 'E', 0x00, 0x00, 0x00, 0x04, 'P', 'F', 'F', '2'
    };

    private boolean broken;
    private final int missingCharacterCode;
    private final int scale;

    private String fontName = "";
    private String family = "";
    private String weight = "";
    private String slant = "";
    private int pointSize;
    private int maxWidth;
    private int maxHeight;
    private int ascent;
    private int descent;

    private final Map<Integer, long[]> characterIndex = new HashMap<>();
    private byte[] data;

    public Pf2Font(InputStream in, int scale, char missingCharacter) throws IOException {
        this.scale = Math.max(1, scale);
        this.missingCharacterCode = missingCharacter;
        try (InputStream input = in) {
            this.data = readAll(input);
        }
        parse();
        if (!broken) {
            pointSize *= this.scale;
            maxWidth *= this.scale;
            maxHeight *= this.scale;
            ascent *= this.scale;
            descent *= this.scale;
        }
    }

    public Pf2Font(InputStream in) throws IOException {
        this(in, 1, '?');
    }

    public boolean isBroken() { return broken; }
    public int pointSize() { return pointSize; }
    public int maxWidth() { return maxWidth; }
    public int maxHeight() { return maxHeight; }
    public int ascent() { return ascent; }
    public int descent() { return descent; }
    public String fontName() { return fontName; }
    public String family() { return family; }
    public int scale() { return scale; }

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private void parse() {
        if (data.length < 12) {
            broken = true;
            return;
        }
        for (int i = 0; i < 12; i++) {
            if (data[i] != PF2_MAGIC[i]) {
                broken = true;
                return;
            }
        }
        int pos = 12;
        while (pos + 8 <= data.length) {
            String name = new String(data, pos, 4, StandardCharsets.US_ASCII);
            int dataLength = int32be(data, pos + 4);
            pos += 8;
            if ("CHIX".equals(name)) {
                int entries = dataLength / (4 + 1 + 4);
                for (int i = 0; i < entries; i++) {
                    int codePoint = int32be(data, pos);
                    int compression = data[pos + 4] & 0xff;
                    int offset = int32be(data, pos + 5);
                    characterIndex.put(codePoint, new long[] {compression, offset});
                    pos += 9;
                }
                continue;
            } else if ("DATA".equals(name)) {
                return;
            }
            if (pos + dataLength > data.length) {
                broken = true;
                return;
            }
            byte[] section = new byte[dataLength];
            System.arraycopy(data, pos, section, 0, dataLength);
            pos += dataLength;
            switch (name) {
                case "NAME": fontName = stripNul(section); break;
                case "FAMI": family = stripNul(section); break;
                case "WEIG": weight = stripNul(section); break;
                case "SLAN": slant = stripNul(section); break;
                case "PTSZ": pointSize = uint16be(section, 0); break;
                case "MAXW": maxWidth = uint16be(section, 0); break;
                case "MAXH": maxHeight = uint16be(section, 0); break;
                case "ASCE": ascent = uint16be(section, 0); break;
                case "DESC": descent = uint16be(section, 0); break;
                default: break;
            }
        }
    }

    private static String stripNul(byte[] b) {
        int len = b.length;
        while (len > 0 && b[len - 1] == 0) {
            len--;
        }
        return new String(b, 0, len, StandardCharsets.UTF_8);
    }

    public Character getChar(int codePoint) {
        long[] info = characterIndex.get(codePoint);
        if (info == null) {
            info = characterIndex.get(missingCharacterCode);
        }
        if (info == null) {
            return null;
        }
        int offset = (int) info[1];
        Character base = readChar(offset);
        if (scale == 1) {
            return base;
        }
        ScaledCharacter sc = new ScaledCharacter();
        sc.scale = scale;
        sc.width = base.width * scale;
        sc.height = base.height * scale;
        sc.deviceWidth = base.deviceWidth * scale;
        sc.xOffset = base.xOffset * scale;
        sc.yOffset = base.yOffset * scale;
        sc.bitmapData = base.bitmapData;
        return sc;
    }

    private Character readChar(int offset) {
        Character ch = new Character();
        ch.width = uint16be(data, offset);
        ch.height = uint16be(data, offset + 2);
        ch.xOffset = int16be(data, offset + 4);
        ch.yOffset = int16be(data, offset + 6);
        ch.deviceWidth = int16be(data, offset + 8);
        int bitmapBytes = (ch.width * ch.height + 7) / 8;
        ch.bitmapData = new byte[bitmapBytes];
        System.arraycopy(data, offset + 10, ch.bitmapData, 0, bitmapBytes);
        return ch;
    }

    private static int uint16be(byte[] b, int off) {
        return ((b[off] & 0xff) << 8) | (b[off + 1] & 0xff);
    }

    private static int int16be(byte[] b, int off) {
        int u = uint16be(b, off);
        return u - ((u >>> 15) << 16);
    }

    private static int int32be(byte[] b, int off) {
        return ((b[off] & 0xff) << 24)
             | ((b[off + 1] & 0xff) << 16)
             | ((b[off + 2] & 0xff) << 8)
             | (b[off + 3] & 0xff);
    }

    public static Pf2Font fromBytes(byte[] data, int scale, char missingCharacter) throws IOException {
        return new Pf2Font(new ByteArrayInputStream(data), scale, missingCharacter);
    }
}
