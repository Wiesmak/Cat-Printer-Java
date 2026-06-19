package com.catprinter.ble.windows;

import com.catprinter.ble.BleDevice;
import com.catprinter.ble.BleTransport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * BLE transport for Windows. Spawns the bundled
 * {@code cat-printer-ble-helper.exe} (extracted from this jar) and drives it
 * over a tiny line-delimited JSON protocol.
 */
public final class WindowsTransport implements BleTransport {

    private static final String HELPER_RESOURCE = "/com/catprinter/ble/windows/cat-printer-ble-helper.exe";
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final HelperProcess helper;
    private final Path helperPath;
    private volatile boolean connected;

    public WindowsTransport() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("windows")) {
            throw new UnsupportedOperationException(
                "WindowsTransport only runs on Windows (os.name=" + System.getProperty("os.name") + ")"
            );
        }
        this.helperPath = extractHelper();
        try {
            ProcessBuilder pb = new ProcessBuilder(helperPath.toString());
            pb.redirectErrorStream(false);
            Process process = pb.start();
            this.helper = new HelperProcess(process);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start BLE helper at " + helperPath, e);
        }
    }

    private static Path extractHelper() {
        try (InputStream in = WindowsTransport.class.getResourceAsStream(HELPER_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(
                    "BLE helper not found on classpath at " + HELPER_RESOURCE +
                    ". Build the helper from windows-helper/ and run the Gradle build to bundle it."
                );
            }
            Path dir = Files.createTempDirectory("cat-printer-ble-");
            Path exe = dir.resolve("cat-printer-ble-helper.exe");
            Files.copy(in, exe, StandardCopyOption.REPLACE_EXISTING);
            exe.toFile().deleteOnExit();
            dir.toFile().deleteOnExit();
            return exe;
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract BLE helper", e);
        }
    }

    @Override
    public List<BleDevice> scan(Duration timeout) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("timeout_ms", (int) timeout.toMillis());
        Map<String, Object> reply;
        try {
            reply = helper.request("scan", params, timeout.plusSeconds(2));
        } catch (IOException e) {
            throw new RuntimeException("scan failed: " + e.getMessage(), e);
        }
        List<BleDevice> result = new ArrayList<>();
        for (Map<String, Object> dev : HelperProcess.devicesFrom(reply)) {
            String name = String.valueOf(dev.getOrDefault("name", ""));
            String address = String.valueOf(dev.get("address"));
            result.add(new BleDevice(name.isEmpty() ? null : name, address));
        }
        return result;
    }

    @Override
    public void connect(BleDevice device, Duration timeout) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("address", device.address());
        try {
            helper.request("connect", params, timeout);
        } catch (IOException e) {
            throw new RuntimeException("connect failed: " + e.getMessage(), e);
        }
        connected = true;
    }

    @Override
    public void disconnect() {
        if (!connected) return;
        try {
            helper.request("disconnect", new LinkedHashMap<>(), DEFAULT_REQUEST_TIMEOUT);
        } catch (IOException e) {
            throw new RuntimeException("disconnect failed: " + e.getMessage(), e);
        } finally {
            connected = false;
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void writeCharacteristic(String characteristicUuid, byte[] data) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("characteristic", characteristicUuid.toLowerCase());
        params.put("data_b64", Base64.getEncoder().encodeToString(data));
        try {
            helper.request("write", params, DEFAULT_REQUEST_TIMEOUT);
        } catch (IOException e) {
            throw new RuntimeException("write failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void subscribeNotifications(String characteristicUuid, Consumer<byte[]> listener) {
        helper.registerSubscription(characteristicUuid, listener);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("characteristic", characteristicUuid.toLowerCase());
        try {
            helper.request("subscribe", params, DEFAULT_REQUEST_TIMEOUT);
        } catch (IOException e) {
            helper.unregisterSubscription(characteristicUuid);
            throw new RuntimeException("subscribe failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void unsubscribeNotifications(String characteristicUuid) {
        helper.unregisterSubscription(characteristicUuid);
    }

    @Override
    public void close() {
        try {
            disconnect();
        } catch (RuntimeException ignored) {
            // best-effort
        }
        helper.close();
        try {
            Files.deleteIfExists(helperPath);
        } catch (IOException ignored) {
            // tmp will be cleaned up on JVM exit
        }
    }
}
