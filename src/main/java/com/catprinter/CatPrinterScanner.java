package com.catprinter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class CatPrinterScanner implements DeviceScanner {
    public CatPrinterScanner (BluetoothAdapter adapter) {
        this.adapter = adapter;
    }

    private final BluetoothAdapter adapter;
    private final ModelRegistry registry = new ModelRegistry();

    @Override
    public List<BluetoothDevice> scan(Duration timeout) {
        return scan(timeout, null);
    }

    @Override
    public List<BluetoothDevice> scan(Duration timeout, String modelFilter) {
        List<BluetoothDevice> all = adapter.scan(timeout);
        List<BluetoothDevice> filtered = new ArrayList<>();
        for (BluetoothDevice d : all) {
            if (registry.isValidModel(d.name())) {
                if (modelFilter == null || (d.name() != null && d.name().startsWith(modelFilter))) {
                    filtered.add(d);
                }
            }
        }
        return filtered;
    }

    @Override
    public void close() {
        // No resources to close in this implementation
    }
}
