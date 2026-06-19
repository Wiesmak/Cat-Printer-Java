package com.catprinter;

import com.catprinter.ble.BleDevice;
import com.catprinter.ble.BleTransport;
import com.catprinter.command.Commander;
import com.catprinter.data.PbmReader;
import com.catprinter.data.PrinterData;
import com.catprinter.text.Pf2Font;
import com.catprinter.text.TextCanvas;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CatPrinter implements AutoCloseable {

    private static final int DEFAULT_MTU = 200;
    private static final long INTER_CHUNK_DELAY_MS = 20;
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);

    private final BleTransport transport;
    private final ModelRegistry registry = new ModelRegistry();
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final ByteArrayOutputStream pending = new ByteArrayOutputStream();
    private final Commander commander;

    private PrinterModel currentModel;
    private int mtu = DEFAULT_MTU;
    private Integer energy;
    private int speed = 32;
    private boolean flipH;
    private boolean flipV;

    public CatPrinter(BleTransport transport) {
        this.transport = transport;
        this.commander = new Commander(this::queue);
    }

    public List<BleDevice> scan(Duration timeout) {
        return scan(timeout, null);
    }

    public List<BleDevice> scan(Duration timeout, String modelFilter) {
        List<BleDevice> all = transport.scan(timeout);
        List<BleDevice> filtered = new ArrayList<>();
        for (BleDevice d : all) {
            if (registry.isValidModel(d.name())) {
                if (modelFilter == null || (d.name() != null && d.name().startsWith(modelFilter))) {
                    filtered.add(d);
                }
            }
        }
        return filtered;
    }

    public void connect(BleDevice device) {
        connect(device, DEFAULT_CONNECT_TIMEOUT);
    }

    public void connect(BleDevice device, Duration timeout) {
        currentModel = registry.getOrUnknown(device.name());
        transport.connect(device, timeout);
        transport.subscribeNotifications(Commander.RX_CHARACTERISTIC, this::handleNotification);
        pending.reset();
        paused.set(false);
    }

    public void setEnergy(int energy) {
        if (energy < 0 || energy > 0xFFFF) {
            throw new IllegalArgumentException("energy out of range 0..0xFFFF: " + energy);
        }
        this.energy = energy;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public void setFlip(boolean horizontal, boolean vertical) {
        this.flipH = horizontal;
        this.flipV = vertical;
    }

    public void setMtu(int mtu) {
        if (mtu < 20) {
            throw new IllegalArgumentException("mtu too small: " + mtu);
        }
        this.mtu = mtu;
    }

    public void printBitmap(byte[] pbmData) {
        printBitmap(new ByteArrayInputStream(pbmData));
    }

    public void printBitmap(InputStream pbm) {
        PrinterModel model = requireModel();
        PrinterData data = new PrinterData(model.paperWidth());
        try {
            PbmReader.read(pbm, data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read PBM: " + e.getMessage(), e);
        }
        data.flip(flipH, flipV);
        prepare(model);
        int rowBytes = model.paperWidth() / 8;
        int total = (int) (data.height() * rowBytes);
        for (int off = 0; off < total; off += rowBytes) {
            byte[] chunk = data.readChunk(off, rowBytes);
            commander.drawBitmap(chunk);
        }
        finish(model);
    }

    public void printText(String text, TextOptions opts, byte[] pf2FontData) {
        PrinterModel model = requireModel();
        Pf2Font font;
        try {
            font = Pf2Font.fromBytes(pf2FontData, opts.scale(), '?');
        } catch (IOException e) {
            throw new RuntimeException("Failed to load PF2 font: " + e.getMessage(), e);
        }
        if (font.isBroken()) {
            throw new IllegalArgumentException("PF2 font is broken");
        }
        TextCanvas canvas = new TextCanvas(model.paperWidth(), font, opts.wrap(), opts.rtl());
        prepare(model);
        int rowBytes = model.paperWidth() / 8;
        for (String line : splitPreservingNewlines(text)) {
            List<byte[]> rendered = canvas.putText(line);
            for (byte[] page : rendered) {
                emitPage(page, rowBytes);
            }
        }
        // Flush any remaining canvas content
        byte[] tail = canvas.flushCanvas();
        emitPage(tail, rowBytes);
        finish(model);
    }

    @Override
    public void close() {
        try {
            transport.unsubscribeNotifications(Commander.RX_CHARACTERISTIC);
        } catch (RuntimeException ignored) {
            // best-effort
        }
        transport.close();
    }

    private void emitPage(byte[] page, int rowBytes) {
        for (int off = 0; off + rowBytes <= page.length; off += rowBytes) {
            byte[] row = Arrays.copyOfRange(page, off, off + rowBytes);
            commander.drawBitmap(row);
        }
    }

    private static List<String> splitPreservingNewlines(String text) {
        List<String> out = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                out.add(text.substring(start, i + 1));
                start = i + 1;
            }
        }
        if (start < text.length()) {
            out.add(text.substring(start));
        }
        return out;
    }

    private void prepare(PrinterModel model) {
        commander.getDeviceState();
        if (model.isNewKind()) {
            commander.startPrintingNew();
        } else {
            commander.startPrinting();
        }
        commander.setDpiAs200();
        if (speed > 0) {
            commander.setSpeed(speed);
        }
        if (energy != null) {
            commander.setEnergy(energy);
        }
        commander.applyEnergy();
        commander.updateDevice();
        flush();
        commander.startLattice();
    }

    private void finish(PrinterModel model) {
        commander.endLattice();
        commander.setSpeed(8);
        if (model.problemFeeding()) {
            byte[] blank = new byte[model.paperWidth() / 8];
            for (int i = 0; i < 128; i++) {
                commander.drawBitmap(blank);
            }
        } else {
            commander.feedPaper(128);
        }
        commander.getDeviceState();
        flush();
    }

    private void queue(byte[] data) {
        pending.write(data, 0, data.length);
        if (pending.size() > mtu * 16 && !paused.get()) {
            flush();
        }
    }

    private void flush() {
        byte[] all = pending.toByteArray();
        pending.reset();
        int off = 0;
        while (off < all.length) {
            while (paused.get()) {
                sleep(50);
            }
            int len = Math.min(mtu, all.length - off);
            byte[] chunk = Arrays.copyOfRange(all, off, off + len);
            transport.writeCharacteristic(Commander.TX_CHARACTERISTIC, chunk);
            off += len;
            sleep(INTER_CHUNK_DELAY_MS);
        }
    }

    private void handleNotification(byte[] data) {
        if (Arrays.equals(data, Commander.DATA_FLOW_PAUSE)) {
            paused.set(true);
        } else if (Arrays.equals(data, Commander.DATA_FLOW_RESUME)) {
            paused.set(false);
        }
    }

    private PrinterModel requireModel() {
        if (currentModel == null) {
            throw new IllegalStateException("Not connected; call connect() first");
        }
        return currentModel;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
