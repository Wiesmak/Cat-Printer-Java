package com.catprinter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelRegistryTest {

    private final ModelRegistry registry = new ModelRegistry();

    @Test
    void exactMatchSc03h() {
        PrinterModel m = registry.get("SC03h");
        assertEquals("SC03h", m.name());
        assertEquals(384, m.paperWidth());
        assertFalse(m.isNewKind());
        assertFalse(m.problemFeeding());
    }

    @Test
    void prefixMatchMxtp01() {
        PrinterModel m = registry.get("MXTP-01");
        assertEquals("MXTP", m.name());
    }

    @Test
    void gb03IsNewKind() {
        assertTrue(registry.get("GB03").isNewKind());
    }

    @Test
    void mx05HasProblemFeeding() {
        assertTrue(registry.get("MX05").problemFeeding());
    }

    @Test
    void unknownNameReturnsFallback() {
        assertNull(registry.get("Nonexistent"));
        assertEquals("_ZZ00", registry.getOrUnknown("Nonexistent").name());
    }

    @Test
    void nullReturnsFallback() {
        assertNull(registry.get(null));
    }

    @Test
    void isValidModelChecks() {
        assertTrue(registry.isValidModel("SC03h"));
        assertTrue(registry.isValidModel("SC03h-extended"));
        assertTrue(registry.isValidModel("MXTP-01"));
        assertFalse(registry.isValidModel("Nonexistent"));
        assertFalse(registry.isValidModel(null));
    }
}
