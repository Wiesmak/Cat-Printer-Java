package com.catprinter;

public final class PrinterModel {

    public static final int DEFAULT_PAPER_WIDTH = 384;

    private final String name;
    private final int paperWidth;
    private final boolean isNewKind;
    private final boolean problemFeeding;

    public PrinterModel(String name, int paperWidth, boolean isNewKind, boolean problemFeeding) {
        this.name = name;
        this.paperWidth = paperWidth;
        this.isNewKind = isNewKind;
        this.problemFeeding = problemFeeding;
    }

    public String name() {
        return name;
    }

    public int paperWidth() {
        return paperWidth;
    }

    public boolean isNewKind() {
        return isNewKind;
    }

    public boolean problemFeeding() {
        return problemFeeding;
    }
}
