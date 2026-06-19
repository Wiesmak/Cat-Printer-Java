package com.catprinter.ble;

import java.util.Objects;

public final class BleDevice {

    private final String name;
    private final String address;

    public BleDevice(String name, String address) {
        this.name = name;
        this.address = Objects.requireNonNull(address, "address");
    }

    public String name() { return name; }
    public String address() { return address; }

    @Override
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
