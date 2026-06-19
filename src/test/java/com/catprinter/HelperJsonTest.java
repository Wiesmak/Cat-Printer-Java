package com.catprinter;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the package-private Json utility used by the Windows transport.
 * Reflection is used because Json is package-private; this keeps the
 * library API surface minimal while still letting us verify the protocol
 * codec.
 */
class HelperJsonTest {

    private static String stringify(Map<String, Object> obj) throws Exception {
        Class<?> json = Class.forName("com.catprinter.ble.windows.Json");
        Method m = json.getDeclaredMethod("stringify", Map.class);
        m.setAccessible(true);
        try {
            return (String) m.invoke(null, obj);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Error er) throw er;
            throw new RuntimeException(cause);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(String s) throws Exception {
        Class<?> json = Class.forName("com.catprinter.ble.windows.Json");
        Method m = json.getDeclaredMethod("parseObject", String.class);
        m.setAccessible(true);
        try {
            return (Map<String, Object>) m.invoke(null, s);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Error er) throw er;
            throw new RuntimeException(cause);
        }
    }

    @Test
    void stringifyScanCommand() throws Exception {
        Map<String, Object> cmd = new LinkedHashMap<>();
        cmd.put("op", "scan");
        cmd.put("id", 1);
        cmd.put("timeout_ms", 4000);
        assertEquals("{\"op\":\"scan\",\"id\":1,\"timeout_ms\":4000}", stringify(cmd));
    }

    @Test
    void stringifyEscapesQuotesAndBackslashes() throws Exception {
        Map<String, Object> cmd = new LinkedHashMap<>();
        cmd.put("error", "bad \"thing\"\\here");
        assertEquals("{\"error\":\"bad \\\"thing\\\"\\\\here\"}", stringify(cmd));
    }

    @Test
    void parseScanReply() throws Exception {
        Map<String, Object> obj = parse(
            "{\"id\": 1, \"result\": \"ok\", \"devices\": [" +
            "{\"name\": \"SC03h\", \"address\": \"AA:BB:CC:DD:EE:FF\"}," +
            "{\"name\": \"\", \"address\": \"11:22:33:44:55:66\"}" +
            "]}"
        );
        assertEquals(1L, ((Number) obj.get("id")).longValue());
        assertEquals("ok", obj.get("result"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> devices = (List<Map<String, Object>>) obj.get("devices");
        assertEquals(2, devices.size());
        assertEquals("SC03h", devices.get(0).get("name"));
        assertEquals("AA:BB:CC:DD:EE:FF", devices.get(0).get("address"));
    }

    @Test
    void parseNotifyEvent() throws Exception {
        Map<String, Object> obj = parse(
            "{\"event\": \"notify\", \"characteristic\": \"0000ae02-0000-1000-8000-00805f9b34fb\"," +
            " \"data_b64\": \"UXi+AAEAEHD/\"}"
        );
        assertEquals("notify", obj.get("event"));
        assertNull(obj.get("id"));
        assertTrue(obj.get("data_b64") instanceof String);
    }

    @Test
    void parseHandlesEscapeSequences() throws Exception {
        Map<String, Object> obj = parse("{\"msg\": \"line1\\nline2\\t\\u0041\"}");
        assertEquals("line1\nline2\tA", obj.get("msg"));
    }

    @Test
    void parseRejectsTrailingGarbage() {
        assertThrows(RuntimeException.class, () -> parse("{\"a\":1} junk"));
    }

    @Test
    void parseRejectsNonObject() {
        assertThrows(RuntimeException.class, () -> parse("[1,2,3]"));
    }

    @Test
    void roundTripBooleansAndArrays() throws Exception {
        Map<String, Object> in = new LinkedHashMap<>();
        in.put("ok", true);
        in.put("none", false);
        in.put("nums", List.of(1, 2, 3));
        String s = stringify(in);
        Map<String, Object> out = parse(s);
        assertEquals(Boolean.TRUE, out.get("ok"));
        assertEquals(Boolean.FALSE, out.get("none"));
        @SuppressWarnings("unchecked")
        List<Object> nums = (List<Object>) out.get("nums");
        assertEquals(3, nums.size());
        assertEquals(1L, ((Number) nums.get(0)).longValue());
    }
}
