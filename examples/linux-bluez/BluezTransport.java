// Starter implementation of BleTransport against com.sputnikdev:bluetooth-manager
// (BlueZ D-Bus). NOT on the library's build path. The sputnikdev API surface
// used here is best-effort and unverified - copy this file into your own
// project, add the dependency, and adjust to whatever the artifact actually
// exposes:
//
//   implementation("com.sputnikdev:bluetooth-manager:1.5.4")
//   implementation("com.sputnikdev:bluetooth-manager-tinyb:1.5.4")
//
package com.catprinter.ble.linux;

import com.catprinter.ble.BleDevice;
import com.catprinter.ble.BleTransport;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.manager.AdapterGovernor;
import org.sputnikdev.bluetooth.manager.BluetoothManager;
import org.sputnikdev.bluetooth.manager.CharacteristicGovernor;
import org.sputnikdev.bluetooth.manager.DeviceGovernor;
import org.sputnikdev.bluetooth.manager.DiscoveredDevice;
import org.sputnikdev.bluetooth.manager.impl.BluetoothManagerBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class BluezTransport implements BleTransport {

    private final BluetoothManager manager;
    private DeviceGovernor device;
    private URL deviceUrl;
    private final Map<String, CharacteristicGovernor> characteristics = new HashMap<>();

    public BluezTransport() {
        this.manager = new BluetoothManagerBuilder()
            .withTinyBTransport(true)
            .withBlueGigaTransport(false)
            .withDiscovering(false)
            .build();
    }

    @Override
    public List<BleDevice> scan(Duration timeout) {
        manager.start(true);
        for (AdapterGovernor adapter : manager.getAdapterGovernors(manager.getRegisteredAdapters())) {
            adapter.setDiscoveringControl(true);
        }
        try {
            Thread.sleep(timeout.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        List<BleDevice> result = new ArrayList<>();
        for (DiscoveredDevice d : manager.getDiscoveredDevices()) {
            URL url = d.getURL();
            result.add(new BleDevice(d.getName(), url.getDeviceAddress()));
        }
        return result;
    }

    @Override
    public void connect(BleDevice bleDevice, Duration timeout) {
        URL adapterUrl = manager.getRegisteredAdapters().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No Bluetooth adapter registered"));
        deviceUrl = adapterUrl.copyWithDevice(bleDevice.address());
        device = manager.getDeviceGovernor(deviceUrl);
        device.setConnectionControl(true);
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (!device.isConnected() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (!device.isConnected()) {
            throw new IllegalStateException("Failed to connect to " + bleDevice);
        }
    }

    @Override
    public void disconnect() {
        if (device != null) {
            device.setConnectionControl(false);
            device = null;
            deviceUrl = null;
            characteristics.clear();
        }
    }

    @Override
    public boolean isConnected() {
        return device != null && device.isConnected();
    }

    @Override
    public void writeCharacteristic(String characteristicUuid, byte[] data) {
        characteristic(characteristicUuid).write(data);
    }

    @Override
    public void subscribeNotifications(String characteristicUuid, Consumer<byte[]> listener) {
        CharacteristicGovernor c = characteristic(characteristicUuid);
        c.addValueListener(listener::accept);
    }

    @Override
    public void unsubscribeNotifications(String characteristicUuid) {
        CharacteristicGovernor c = characteristics.remove(characteristicUuid);
        if (c != null) {
            // sputnikdev removes listeners via the governor's remove API; specific
            // listener references aren't tracked here, so we just drop the cached
            // governor — the connection close in close() will tear listeners down.
        }
    }

    @Override
    public void close() {
        disconnect();
        manager.dispose();
    }

    private CharacteristicGovernor characteristic(String uuid) {
        if (deviceUrl == null) {
            throw new IllegalStateException("Not connected");
        }
        return characteristics.computeIfAbsent(uuid.toLowerCase(),
            u -> manager.getCharacteristicGovernor(deviceUrl.copyWithCharacteristic(u)));
    }
}
