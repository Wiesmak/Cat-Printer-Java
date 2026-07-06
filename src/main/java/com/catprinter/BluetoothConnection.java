package com.catprinter;

import java.time.Duration;
import java.util.function.Consumer;

public interface BluetoothConnection extends AutoCloseable {
    void connect(BluetoothDevice device, Duration timeout);

    void disconnect();

    boolean isConnected();

    void writeCharacteristic(String characteristicUuid, byte[] data);

    void subscribeNotifications(String characteristicUuid, Consumer<byte[]> listener);

    void unsubscribeNotifications(String characteristicUuid);

    @Override
    void close();
}
