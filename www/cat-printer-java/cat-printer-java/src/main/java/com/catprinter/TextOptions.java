package com.catprinter;

public final class TextOptions {

    private final boolean wrap;
    private final boolean rtl;
    private final int scale;

    public TextOptions(boolean wrap, boolean rtl, int scale) {
        this.wrap = wrap;
        this.rtl = rtl;
        this.scale = Math.max(1, scale);
    }

    public static TextOptions defaults() {
        return new TextOptions(true, false, 1);
    }

    public boolean wrap() { return wrap; }
    public boolean rtl() { return rtl; }
    public int scale() { return scale; }
}
