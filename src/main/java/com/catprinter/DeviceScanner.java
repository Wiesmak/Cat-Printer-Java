package com.catprinter;

import java.time.Duration;
import java.util.List;

public interface DeviceScanner extends AutoCloseable {
    List<BluetoothDevice> scan(Duration timeout);

    List<BluetoothDevice> scan(Duration timeout, String modelFilter);
}
