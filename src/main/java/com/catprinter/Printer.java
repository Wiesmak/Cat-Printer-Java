package com.catprinter;

import com.catprinter.ble.BleDevice;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;

public interface Printer extends AutoCloseable {
    List<BleDevice> scan(Duration timeout);

    List<BleDevice> scan(Duration timeout, String modelFilter);

    void connect(BleDevice device);

    void connect(BleDevice device, Duration timeout);

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
