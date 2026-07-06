package com.catprinter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Demo {
    public static void main(String[] args) throws Exception {
        try (PrintingManager manager = new CatPrinterManager()) {
            List<BluetoothDevice> devices = manager.discoverPrinters();
            System.out.println("Found" + devices);
            if (devices.isEmpty()) return;

            byte[] pbm = Files.readAllBytes(Path.of(args[0]));
            manager.printBitmap(devices.get(0), pbm);
        }
    }
}