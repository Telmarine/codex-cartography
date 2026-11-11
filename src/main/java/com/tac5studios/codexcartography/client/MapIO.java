package com.tac5studios.codexcartography.client;

import com.tac5studios.codexcartography.CodexCartography;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** One background thread for every map file read and write, so the game never waits on the disk. Jobs run in order. */
public final class MapIO {

    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Codex Cartography IO");
        t.setDaemon(true);
        return t;
    });

    private MapIO() {}

    public static void run(String what, IoJob job) {
        IO.execute(() -> {
            try {
                job.run();
            } catch (Exception e) {
                CodexCartography.LOGGER.warn("[Cartography] Couldn't {}: {}", what, e.toString());
            }
        });
    }

    /** Waits (up to the time given) for every job queued so far to finish. */
    public static void await(long millis) {
        try {
            IO.submit(() -> {}).get(millis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            CodexCartography.LOGGER.warn("[Cartography] Map saving is taking longer than expected; it will finish in the background.");
        }
    }

    @FunctionalInterface
    public interface IoJob {
        void run() throws Exception;
    }
}
