package com.catprinter;

import java.io.InputStream;
import java.util.List;

public interface PrintingManager extends AutoCloseable {
    List<BluetoothDevice> discoverPrinters();

    void printBitmap(BluetoothDevice device, byte[] pbmData);

    void printBitmap(BluetoothDevice device, InputStream pbm);

    void printText(BluetoothDevice device, String text);

    @Override
    void close();
}
