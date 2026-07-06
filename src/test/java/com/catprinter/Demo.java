package com.catprinter;

import com.catprinter.ble.BleDevice;
import com.catprinter.ble.BleTransport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public class Demo {
    public static void main(String[] args) throws Exception {
        try (BleTransport transport = BleTransport.platformDefault();
            Printer printer = new CatPrinter(transport)) {

                List<BleDevice> devices = printer.scan(Duration.ofSeconds(4));
                System.out.println("Found" + devices);
                if (devices.isEmpty()) return;

                printer.connect(devices.get(0));
                printer.setEnergy(0x4000);
                printer.setSpeed(36);

                byte[] pbm = Files.readAllBytes(Path.of(args[0]));
                printer.printBitmap(pbm);
            }
    }
}