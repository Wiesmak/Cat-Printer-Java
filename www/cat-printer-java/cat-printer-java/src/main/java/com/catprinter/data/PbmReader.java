package com.catprinter.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;

public final class PbmReader {

    private PbmReader() {}

    public static void read(InputStream in, PrinterData target) throws IOException {
        PushbackInputStream pin = new PushbackInputStream(in, 1);
        while (true) {
            String signature = readLine(pin);
            if (signature == null) {
                return;
            }
            if (!signature.equals("P4")) {
                throw new IOException("input-is-not-pbm-image");
            }
            String dims;
            while (true) {
                dims = readLine(pin);
                if (dims == null) {
                    throw new IOException("broken-pbm-image");
                }
                if (!dims.startsWith("#")) {
                    break;
                }
            }
            String[] parts = dims.trim().split("\\s+");
            if (parts.length != 2) {
                throw new IOException("broken-pbm-image");
            }
            int width;
            int height;
            try {
                width = Integer.parseInt(parts[0]);
                height = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                throw new IOException("broken-pbm-image", e);
            }
            if (width != target.width()) {
                throw new IOException("unsuitable-image-width-expected-" + target.width() + "-got-" + width);
            }
            target.addPage(height);
            int expected = target.dataWidth() * height;
            byte[] buf = new byte[Math.min(4 * 1024 * 1024, expected)];
            int total = 0;
            while (total < expected) {
                int n = pin.read(buf, 0, Math.min(buf.length, expected - total));
                if (n < 0) {
                    break;
                }
                target.write(buf, 0, n);
                total += n;
            }
            if (total != expected) {
                throw new IOException("broken-pbm-image");
            }
        }
    }

    private static String readLine(PushbackInputStream in) throws IOException {
        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        int c = in.read();
        if (c < 0) {
            return null;
        }
        while (c >= 0 && c != '\n') {
            buf.write(c);
            c = in.read();
        }
        return buf.toString(StandardCharsets.US_ASCII);
    }
}
