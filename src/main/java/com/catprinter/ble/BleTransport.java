package com.catprinter.ble;

import com.catprinter.BluetoothAdapter;
import com.catprinter.BluetoothConnection;
import com.catprinter.BluetoothDevice;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

public interface BleTransport extends BluetoothConnection, BluetoothAdapter {

    List<BluetoothDevice> scan(Duration timeout);

    void connect(BluetoothDevice device, Duration timeout);

    void disconnect();

    boolean isConnected();

    void writeCharacteristic(String characteristicUuid, byte[] data);

    void subscribeNotifications(String characteristicUuid, Consumer<byte[]> listener);

    void unsubscribeNotifications(String characteristicUuid);

    @Override
    void close();

    static BleTransport platformDefault() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("windows")) {
            try {
                Class<?> impl = Class.forName("com.catprinter.ble.windows.WindowsTransport");
                return (BleTransport) impl.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                throw new UnsupportedOperationException(
                    "Windows BLE backend failed to start: " + cause.getMessage(), cause
                );
            }
        }
        throw new UnsupportedOperationException(
            "No built-in BLE backend (os=" + os + "). " +
            "Provide a custom BleTransport implementation. " +
            "See examples/linux-bluez/BluezTransport.java for a Linux starter."
        );
    }
}
