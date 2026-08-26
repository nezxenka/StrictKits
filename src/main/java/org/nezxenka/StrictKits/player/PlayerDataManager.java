package org.nezxenka.StrictKits.player;

import org.nezxenka.StrictKits.storage.DataEntry;
import org.nezxenka.StrictKits.storage.DatabaseConfig;
import org.nezxenka.StrictKits.storage.PlayerRecord;
import org.nezxenka.StrictKits.storage.StorageProvider;
import org.nezxenka.StrictKits.storage.cache.CacheProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PlayerDataManager {

    private final StorageProvider storage;
    private final CacheProvider cache;
    private final DatabaseConfig config;
    private final Logger logger;

    private final ConcurrentHashMap<UUID, PlayerData> loaded = new ConcurrentHashMap<>();
    private final ExecutorService workers;
    private final ScheduledExecutorService scheduler;

    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong cacheMisses = new AtomicLong();
    private final AtomicLong writes = new AtomicLong();

    public PlayerDataManager(StorageProvider storage, CacheProvider cache, DatabaseConfig config, Logger logger) {
        this.storage = storage;
        this.cache = cache;
        this.config = config;
        this.logger = logger;
        this.workers = Executors.newFixedThreadPool(config.getWorkerThreads(), namedFactory("StrictKits-Worker"));
        this.scheduler = Executors.newSingleThreadScheduledExecutor(namedFactory("StrictKits-Flush"));
    }

    private static ThreadFactory namedFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    public void start() {
        long period = config.getFlushIntervalSeconds();
        scheduler.scheduleWithFixedDelay(this::flushSafely, period, period, TimeUnit.SECONDS);
        long unloadPeriod = Math.max(30, config.getUnloadDelaySeconds());
        scheduler.scheduleWithFixedDelay(this::evictOffline, unloadPeriod, unloadPeriod, TimeUnit.SECONDS);
        cache.setKitInvalidationListener(this::onRemoteKitRemoved);
    }

    public ExecutorService getWorkers() {
        return workers;
    }

    public PlayerData get(UUID uuid) {
        PlayerData data = loaded.get(uuid);
        if (data != null) {
            data.touch();
        }
        return data;
    }

    public boolean isLoaded(UUID uuid) {
        return loaded.containsKey(uuid);
    }

    public PlayerData preload(UUID uuid) {
        PlayerData existing = loaded.get(uuid);
        if (existing != null) {
            existing.touch();
            return existing;
        }
        PlayerData data = fetch(uuid);
        PlayerData previous = loaded.putIfAbsent(uuid, data);
        return previous == null ? data : previous;
    }

    public PlayerData markOnline(UUID uuid) {
        PlayerData data = preload(uuid);
        data.setOnline(true);
        return data;
    }

    private PlayerData fetch(UUID uuid) {
        long started = System.nanoTime();
        try {
            PlayerRecord cached = cache.get(uuid);
            if (cached != null) {
                cacheHits.incrementAndGet();
                return PlayerData.from(cached);
            }
            cacheMisses.incrementAndGet();
            PlayerRecord record = storage.load(uuid);
            cache.put(record);
            return PlayerData.from(record);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Не удалось загрузить данные игрока " + uuid, e);
            return new PlayerData(uuid);
        } finally {
            long elapsed = (System.nanoTime() - started) / 1000000L;
            if (elapsed > config.getLoadTimeoutMillis()) {
                logger.warning("Загрузка данных " + uuid + " заняла " + elapsed + "ms");
            }
        }
    }

    public void handleQuit(UUID uuid) {
        PlayerData data = loaded.get(uuid);
        if (data == null) {
            return;
        }
        data.setOnline(false);
        if (config.isSaveOnQuit() && data.isDirty()) {
            workers.execute(() -> flushSingle(data));
        }
    }

    private void evictOffline() {
        long threshold = System.currentTimeMillis() - config.getUnloadDelaySeconds() * 1000L;
        Iterator<Map.Entry<UUID, PlayerData>> iterator = loaded.entrySet().iterator();
        while (iterator.hasNext()) {
            PlayerData data = iterator.next().getValue();
            if (data.isOnline() || data.getLastAccess() > threshold) {
                continue;
            }
            if (data.isDirty()) {
                flushSingle(data);
            }
            iterator.remove();
        }
    }

    private void flushSafely() {
        try {
            flush();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при сохранении данных игроков", e);
        }
    }

    public void flush() {
        if (loaded.isEmpty()) {
            return;
        }
        List<DataEntry> cooldownBatch = new ArrayList<>();
        List<DataEntry> claimBatch = new ArrayList<>();
        List<PlayerData> touched = new ArrayList<>();
        List<List<String>> cooldownKeys = new ArrayList<>();
        List<List<String>> claimKeys = new ArrayList<>();

        for (PlayerData data : loaded.values()) {
            if (!data.isDirty()) {
                continue;
            }
            List<String> cooldownDirty = data.drainDirtyCooldowns();
            List<String> claimDirty = data.drainDirtyClaims();
            if (cooldownDirty == null && claimDirty == null) {
                continue;
            }
            touched.add(data);
            cooldownKeys.add(cooldownDirty);
            claimKeys.add(claimDirty);
            appendEntries(cooldownBatch, data, cooldownDirty);
            appendEntries(claimBatch, data, claimDirty);
        }

        if (touched.isEmpty()) {
            return;
        }

        try {
            storage.writeCooldowns(cooldownBatch);
            storage.writeClaims(claimBatch);
            writes.addAndGet(cooldownBatch.size() + claimBatch.size());
            for (PlayerData data : touched) {
                cache.put(data.toRecord());
            }
        } catch (Exception e) {
            for (int i = 0; i < touched.size(); i++) {
                List<String> cooldownDirty = cooldownKeys.get(i);
                List<String> claimDirty = claimKeys.get(i);
                if (cooldownDirty != null) {
                    touched.get(i).restoreDirtyCooldowns(cooldownDirty);
                }
                if (claimDirty != null) {
                    touched.get(i).restoreDirtyClaims(claimDirty);
                }
            }
            logger.log(Level.SEVERE, "Не удалось записать пакет данных, повтор при следующем сбросе", e);
        }
    }

    private static void appendEntries(List<DataEntry> target, PlayerData data, List<String> keys) {
        if (keys == null) {
            return;
        }
        long fallback = System.currentTimeMillis();
        for (String key : keys) {
            long stamp = data.getCooldown(key);
            target.add(new DataEntry(data.getUuid(), key, stamp == 0L ? fallback : stamp));
        }
    }

    private void flushSingle(PlayerData data) {
        List<String> cooldownDirty = data.drainDirtyCooldowns();
        List<String> claimDirty = data.drainDirtyClaims();
        if (cooldownDirty == null && claimDirty == null) {
            return;
        }
        List<DataEntry> cooldownBatch = new ArrayList<>(cooldownDirty == null ? 0 : cooldownDirty.size());
        List<DataEntry> claimBatch = new ArrayList<>(claimDirty == null ? 0 : claimDirty.size());
        appendEntries(cooldownBatch, data, cooldownDirty);
        appendEntries(claimBatch, data, claimDirty);
        try {
            storage.writeCooldowns(cooldownBatch);
            storage.writeClaims(claimBatch);
            writes.addAndGet(cooldownBatch.size() + claimBatch.size());
            cache.put(data.toRecord());
        } catch (Exception e) {
            if (cooldownDirty != null) {
                data.restoreDirtyCooldowns(cooldownDirty);
            }
            if (claimDirty != null) {
                data.restoreDirtyClaims(claimDirty);
            }
            logger.log(Level.SEVERE, "Не удалось сохранить данные игрока " + data.getUuid(), e);
        }
    }

    public void onKitRemoved(String kitKey) {
        for (PlayerData data : loaded.values()) {
            data.forgetKit(kitKey);
        }
        workers.execute(() -> {
            try {
                storage.deleteKit(kitKey);
                cache.invalidateKit(kitKey);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Не удалось удалить данные кита " + kitKey, e);
            }
        });
    }

    public void purgeExpired(String kitKey, long cooldownMillis) {
        if (cooldownMillis <= 0L) {
            return;
        }
        workers.execute(() -> {
            try {
                int removed = storage.purgeCooldowns(kitKey, System.currentTimeMillis() - cooldownMillis);
                if (removed > 0) {
                    logger.info("Очищено " + removed + " истёкших кулдаунов кита " + kitKey);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Не удалось очистить кулдауны кита " + kitKey, e);
            }
        });
    }

    private void onRemoteKitRemoved(String kitKey) {
        for (PlayerData data : loaded.values()) {
            data.forgetKit(kitKey);
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
        flushSafely();
        workers.shutdown();
        try {
            if (!workers.awaitTermination(15L, TimeUnit.SECONDS)) {
                workers.shutdownNow();
            }
        } catch (InterruptedException e) {
            workers.shutdownNow();
            Thread.currentThread().interrupt();
        }
        loaded.clear();
    }

    public int getLoadedCount() {
        return loaded.size();
    }

    public long getCacheHits() {
        return cacheHits.get();
    }

    public long getCacheLookups() {
        return cacheHits.get() + cacheMisses.get();
    }

    public long getCacheHitRatio() {
        long hits = cacheHits.get();
        long total = hits + cacheMisses.get();
        return total == 0L ? 0L : hits * 100L / total;
    }

    public long getWrites() {
        return writes.get();
    }
}
