package com.catprinter;

import java.time.Duration;
import java.util.List;

public interface BluetoothAdapter {
    List<BluetoothDevice> scan(Duration timeout);
}
