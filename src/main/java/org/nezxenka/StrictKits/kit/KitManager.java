package org.nezxenka.StrictKits.kit;

import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public final class KitManager {

    private static final Comparator<Kit> BY_NAME = Comparator.comparing(Kit::getName, String.CASE_INSENSITIVE_ORDER);

    private final KitStorage storage;
    private final Map<String, Kit> kits = new ConcurrentHashMap<>();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "StrictKits-KitIO");
        thread.setDaemon(true);
        return thread;
    });

    private volatile List<Kit> snapshot = List.of();
    private volatile List<String> nameSnapshot = List.of();

    public int loadAll() {
        kits.clear();
        storage.loadAll().forEach(kit -> kits.put(kit.getKey(), kit));
        rebuildSnapshot();
        return kits.size();
    }

    public Kit get(String name) {
        return name == null || name.isEmpty() ? null : kits.get(name.toLowerCase());
    }

    public List<Kit> all() {
        return snapshot;
    }

    public List<String> names() {
        return nameSnapshot;
    }

    public int size() {
        return kits.size();
    }

    public Kit create(String name) {
        Kit kit = new Kit(name);
        if (kits.putIfAbsent(kit.getKey(), kit) != null) {
            return null;
        }
        rebuildSnapshot();
        kit.markDirty();
        flush(kit);
        return kit;
    }

    public void remove(Kit kit) {
        if (kits.remove(kit.getKey(), kit)) {
            rebuildSnapshot();
            kit.consumeDirty();
            submit(() -> storage.delete(kit));
        }
    }

    public void flush(Kit kit) {
        if (kit.consumeDirty()) {
            submit(() -> storage.save(kit));
        }
    }

    public void flushAllBlocking() {
        for (Kit kit : snapshot) {
            if (kit.consumeDirty()) {
                storage.save(kit);
            }
        }
    }

    public void shutdown() {
        ioExecutor.shutdown();
        try {
            if (!ioExecutor.awaitTermination(10L, TimeUnit.SECONDS)) {
                ioExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            ioExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void submit(Runnable task) {
        if (ioExecutor.isShutdown()) {
            task.run();
        } else {
            ioExecutor.execute(task);
        }
    }

    private void rebuildSnapshot() {
        List<Kit> sorted = kits.values().stream().sorted(BY_NAME).toList();
        snapshot = sorted;
        nameSnapshot = sorted.stream().map(Kit::getName).toList();
    }
}
