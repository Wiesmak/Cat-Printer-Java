package com.catprinter;

public record TextOptions(
    boolean wrap,
    boolean rtl,
    int scale
) {

    public TextOptions(boolean wrap, boolean rtl, int scale) {
        this.wrap = wrap;
        this.rtl = rtl;
        this.scale = Math.max(1, scale);
    }

    public static TextOptions defaults() {
        return new TextOptions(true, false, 1);
    }
}
