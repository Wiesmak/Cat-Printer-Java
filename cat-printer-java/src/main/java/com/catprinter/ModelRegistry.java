package com.catprinter;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModelRegistry {

    public static final String UNKNOWN_NAME = "_ZZ00";

    private static final String[] ALL_MODELS = {
        "_ZZ00", "GB01", "GB02", "GB03", "GT01",
        "MX05", "MX06", "MX08", "MX09", "MX10",
        "YT01", "MX11", "SC03h", "MXTP"
    };

    private static final String[] NEW_KIND = {"GB03"};
    private static final String[] PROBLEM_FEEDING = {"MX05", "MX06", "MX08", "MX09", "MX10"};

    private final Map<String, PrinterModel> models = new LinkedHashMap<>();

    public ModelRegistry() {
        for (String name : ALL_MODELS) {
            boolean newKind = contains(NEW_KIND, name);
            boolean badFeed = contains(PROBLEM_FEEDING, name);
            models.put(name, new PrinterModel(name, PrinterModel.DEFAULT_PAPER_WIDTH, newKind, badFeed));
        }
    }

    private static boolean contains(String[] arr, String value) {
        for (String s : arr) {
            if (s.equals(value)) {
                return true;
            }
        }
        return false;
    }

    public PrinterModel get(String name) {
        return get(name, null);
    }

    public PrinterModel get(String name, PrinterModel fallback) {
        if (name == null) {
            return fallback;
        }
        PrinterModel exact = models.get(name);
        if (exact != null) {
            return exact;
        }
        String bestMatch = null;
        for (String key : models.keySet()) {
            if (name.startsWith(key) && (bestMatch == null || key.length() > bestMatch.length())) {
                bestMatch = key;
            }
        }
        if (bestMatch != null) {
            return models.get(bestMatch);
        }
        return fallback;
    }

    public PrinterModel getOrUnknown(String name) {
        return get(name, models.get(UNKNOWN_NAME));
    }

    public boolean isValidModel(String name) {
        if (name == null) {
            return false;
        }
        for (String key : models.keySet()) {
            if (name.startsWith(key)) {
                return true;
            }
        }
        return false;
    }
}
