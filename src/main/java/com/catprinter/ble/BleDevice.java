package com.catprinter.ble;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record BleDevice(
    String name,
    String address
) implements com.catprinter.BluetoothDevice {

    public BleDevice(String name, String address) {
        this.name = name;
        this.address = Objects.requireNonNull(address, "address");
    }

    @Override
    @NotNull
    public String toString() {
        return (name == null ? "<unnamed>" : name) + " (" + address + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BleDevice other)) return false;
        return address.equalsIgnoreCase(other.address);
    }

    @Override
    public int hashCode() {
        return address.toLowerCase().hashCode();
    }
}
