package com.catprinter.ble.windows;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Drives the cat-printer-ble-helper.exe over stdin/stdout. Sends one JSON
 * command per line, awaits a reply with a matching {@code id}, and dispatches
 * unsolicited {@code notify} events to subscribed listeners.
 */
final class HelperProcess implements AutoCloseable {

    private final Process process;
    private final BufferedWriter stdin;
    private final Thread reader;
    private final Thread errDrain;

    private final AtomicInteger nextId = new AtomicInteger(1);
    private final Map<Integer, CompletableFuture<Map<String, Object>>> pending = new ConcurrentHashMap<>();
    private final Map<String, Consumer<byte[]>> subscriptions = new ConcurrentHashMap<>();
    private volatile boolean closed;

    HelperProcess(Process process) {
        this.process = process;
        this.stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        this.reader = new Thread(this::readerLoop, "cat-printer-helper-reader");
        this.reader.setDaemon(true);
        this.errDrain = new Thread(() -> drain(process.getErrorStream()), "cat-printer-helper-stderr");
        this.errDrain.setDaemon(true);
        this.reader.start();
        this.errDrain.start();
    }

    Map<String, Object> request(String op, Map<String, Object> params, Duration timeout)
            throws IOException {
        if (closed) throw new IOException("HelperProcess is closed");
        int id = nextId.getAndIncrement();
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        pending.put(id, future);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("op", op);
        body.put("id", id);
        if (params != null) body.putAll(params);
        try {
            send(body);
        } catch (IOException e) {
            pending.remove(id);
            throw e;
        }
        try {
            Map<String, Object> reply = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            String result = (String) reply.get("result");
            if (!"ok".equals(result)) {
                throw new IOException("helper-error: " + reply.get("error"));
            }
            return reply;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted waiting for helper reply", e);
        } catch (TimeoutException e) {
            pending.remove(id);
            throw new IOException("timeout waiting for helper reply (op=" + op + ")", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException io) throw io;
            throw new IOException("helper-failed: " + cause.getMessage(), cause);
        }
    }

    void registerSubscription(String characteristicUuid, Consumer<byte[]> listener) {
        subscriptions.put(characteristicUuid.toLowerCase(), listener);
    }

    void unregisterSubscription(String characteristicUuid) {
        subscriptions.remove(characteristicUuid.toLowerCase());
    }

    private synchronized void send(Map<String, Object> body) throws IOException {
        stdin.write(Json.stringify(body));
        stdin.write('\n');
        stdin.flush();
    }

    private void readerLoop() {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                handleLine(line);
            }
        } catch (IOException e) {
            // process exited or stream broken; fall through to fail-pending
        } finally {
            failAllPending(new IOException("helper process exited"));
        }
    }

    private void handleLine(String line) {
        Map<String, Object> obj;
        try {
            obj = Json.parseObject(line);
        } catch (RuntimeException e) {
            return;
        }
        if (obj.containsKey("id")) {
            int id = ((Number) obj.get("id")).intValue();
            CompletableFuture<Map<String, Object>> f = pending.remove(id);
            if (f != null) f.complete(obj);
            return;
        }
        if ("notify".equals(obj.get("event"))) {
            String uuid = String.valueOf(obj.get("characteristic")).toLowerCase();
            String b64 = String.valueOf(obj.get("data_b64"));
            Consumer<byte[]> listener = subscriptions.get(uuid);
            if (listener != null) {
                listener.accept(java.util.Base64.getDecoder().decode(b64));
            }
        }
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> devicesFrom(Map<String, Object> reply) {
        Object devices = reply.get("devices");
        if (devices instanceof List<?> l) return (List<Map<String, Object>>) l;
        return List.of();
    }

    private void drain(java.io.InputStream in) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.err.println("[ble-helper] " + line);
            }
        } catch (IOException ignored) {
            // process gone
        }
    }

    private void failAllPending(IOException cause) {
        for (Map.Entry<Integer, CompletableFuture<Map<String, Object>>> e : pending.entrySet()) {
            e.getValue().completeExceptionally(cause);
        }
        pending.clear();
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("op", "exit");
            send(body);
        } catch (IOException ignored) {
            // process probably already gone
        }
        try {
            stdin.close();
        } catch (IOException ignored) {}
        if (!process.isAlive()) return;
        try {
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroy();
            }
            if (!process.waitFor(1, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }
}
