package com.catprinter;

import com.catprinter.ble.BleTransport;

import java.io.InputStream;
import java.util.List;

public final class CatPrinterManager implements PrintingManager {
    private final BleTransport transport;
    public final CatPrinterScanner scanner;

    public CatPrinterManager() {
        this.transport = BleTransport.platformDefault();
        this.scanner = new CatPrinterScanner(transport);
    }

    @Override
    public List<BluetoothDevice> discoverPrinters() {
        return scanner.scan(java.time.Duration.ofSeconds(4));
    }

    @Override
    public void printBitmap(BluetoothDevice device, byte[] pbmData) {
        handlePrintBitmap(device, pbmData, null, PrintingOptions.defaults());
    }

    @Override
    public void printBitmap(BluetoothDevice device, InputStream pbm) {
        handlePrintBitmap(device, null, pbm, PrintingOptions.defaults());
    }

    @Override
    public void printText(BluetoothDevice device, String text) {
        handlePrintText(device, text, PrintingOptions.defaults(), TextOptions.defaults(), null);
    }

    @Override
    public void close() {
        scanner.close();
        transport.close();
    }

    private void handlePrintBitmap(BluetoothDevice device, byte[] pbmData, InputStream pbm, PrintingOptions options) {
        assert pbmData != null || pbm != null : "Either pbmData or pbm must be provided";

        try (CatPrinter printer = new CatPrinter(transport)) {
            printer.connect(device);
            printer.setEnergy(options.energy());
            printer.setSpeed(options.speed());
            printer.setFlip(options.flipHorizontal(), options.flipVertical());
            printer.setMtu(options.mtu());
            if (pbmData != null) {
                printer.printBitmap(pbmData);
            } else {
                printer.printBitmap(pbm);
            }
        }
    }

    private void handlePrintText(BluetoothDevice device, String text, PrintingOptions options, TextOptions textOptions, byte[] fontData) {
        try (CatPrinter printer = new CatPrinter(transport)) {
            printer.connect(device);
            printer.setEnergy(options.energy());
            printer.setSpeed(options.speed());
            printer.setFlip(options.flipHorizontal(), options.flipVertical());
            printer.setMtu(options.mtu());
            printer.printText(text, textOptions, fontData);
        }
    }
}
