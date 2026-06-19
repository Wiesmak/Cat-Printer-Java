package com.catprinter;

import com.catprinter.ble.BleTransport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertThrows;

class WindowsTransportTest {

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void platformDefaultThrowsOnNonWindows() {
        // On macOS/Linux the platform default should still throw; the helper
        // is Windows-only and the SPI keeps requiring a custom impl elsewhere.
        assertThrows(UnsupportedOperationException.class, BleTransport::platformDefault);
    }
}
