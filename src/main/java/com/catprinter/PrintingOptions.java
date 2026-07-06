package com.catprinter;

public record PrintingOptions(
    int energy,
    int speed,
    boolean flipHorizontal,
    boolean flipVertical,
    int mtu
) {
    public PrintingOptions {
        if (energy < 0 || energy > 0xFFFF) {
            throw new IllegalArgumentException("Energy must be between 0 and 100");
        }
        if (speed < 0 || speed > 100) {
            throw new IllegalArgumentException("Speed must be between 0 and 100");
        }
        if (mtu < 20 || mtu > 512) {
            throw new IllegalArgumentException("MTU must be between 20 and 512");
        }
    }

    public PrintingOptions withEnergy(int energy) {
        return new PrintingOptions(energy, this.speed, this.flipHorizontal, this.flipVertical, this.mtu);
    }

    public PrintingOptions withSpeed(int speed) {
        return new PrintingOptions(this.energy, speed, this.flipHorizontal, this.flipVertical, this.mtu);
    }

    public PrintingOptions withFlip(boolean flipHorizontal, boolean flipVertical) {
        return new PrintingOptions(this.energy, this.speed, flipHorizontal, flipVertical, this.mtu);
    }

    public PrintingOptions withMtu(int mtu) {
        return new PrintingOptions(this.energy, this.speed, this.flipHorizontal, this.flipVertical, mtu);
    }

    public static PrintingOptions defaults() {
        return new PrintingOptions(0x4000, 36, false, false, 200);
    }
}
