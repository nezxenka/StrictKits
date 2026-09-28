package org.nezxenka.strictkits.kit.registry;

import lombok.RequiredArgsConstructor;
import org.nezxenka.strictkits.kit.model.Kit;
import org.nezxenka.strictkits.kit.naming.KitNames;
import org.nezxenka.strictkits.kit.storage.KitFileStorage;
import org.nezxenka.strictkits.util.concurrent.DaemonThreadFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public final class KitRegistry {

    private static final Comparator<Kit> BY_NAME = Comparator.comparing(Kit::getName, String.CASE_INSENSITIVE_ORDER);
    private static final long IO_TIMEOUT_SECONDS = 10L;

    private final KitFileStorage storage;
    private final Logger logger;
    private final Map<String, Kit> kits = new ConcurrentHashMap<>();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(new DaemonThreadFactory("StrictKits-KitIO"));

    private volatile List<Kit> snapshot = List.of();
    private volatile List<String> nameSnapshot = List.of();

    public int loadAll() {
        kits.clear();
        storage.loadAll().forEach(kit -> kits.put(kit.getKey(), kit));
        rebuildSnapshot();
        return kits.size();
    }

    public Kit get(String name) {
        return name == null || name.isEmpty() ? null : kits.get(KitNames.key(name));
    }

    public boolean contains(Kit kit) {
        return kits.get(kit.getKey()) == kit;
    }

    public List<Kit> all() {
        return snapshot;
    }

    public List<String> names() {
        return nameSnapshot;
    }

    public List<String> namesWithoutIcon() {
        return snapshot.stream().filter(kit -> !kit.hasIcon()).map(Kit::getName).toList();
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
        save(kit);
        return kit;
    }

    public void remove(Kit kit) {
        if (kits.remove(kit.getKey(), kit)) {
            rebuildSnapshot();
            submit(() -> storage.delete(kit));
        }
    }

    public void save(Kit kit) {
        submit(() -> storage.save(kit));
    }

    public void awaitPendingWrites() {
        try {
            ioExecutor.submit(() -> {
            }).get(IO_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            logger.log(Level.WARNING, "Не дождались сохранения китов перед перезагрузкой", e);
        }
    }

    public void shutdown() {
        ioExecutor.shutdown();
        try {
            if (!ioExecutor.awaitTermination(IO_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
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
