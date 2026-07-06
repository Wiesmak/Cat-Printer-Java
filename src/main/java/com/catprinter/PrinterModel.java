package com.catprinter;

public record PrinterModel(
    String name,
    int paperWidth,
    boolean isNewKind,
    boolean problemFeeding
) {

    public static final int DEFAULT_PAPER_WIDTH = 384;

}
