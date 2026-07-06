package com.catprinter;

import java.io.InputStream;
import java.time.Duration;

public interface Printer extends AutoCloseable {
    void connect(BluetoothDevice device);

    void connect(BluetoothDevice device, Duration timeout);

    void setEnergy(int energy);

    void setSpeed(int speed);

    void setFlip(boolean horizontal, boolean vertical);

    void setMtu(int mtu);

    void printBitmap(byte[] pbmData);

    void printBitmap(InputStream pbm);

    void printText(String text, TextOptions opts, byte[] pf2FontData);

    @Override
    void close();
}
