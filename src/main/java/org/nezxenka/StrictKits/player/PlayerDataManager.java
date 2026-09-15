package org.nezxenka.StrictKits.player;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.nezxenka.StrictKits.storage.DataEntry;
import org.nezxenka.StrictKits.storage.DatabaseConfig;
import org.nezxenka.StrictKits.storage.PlayerRecord;
import org.nezxenka.StrictKits.storage.StorageProvider;
import org.nezxenka.StrictKits.storage.cache.CacheProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private static final long MIN_UNLOAD_PERIOD_SECONDS = 30L;

    private final StorageProvider storage;
    private final CacheProvider cache;
    private final DatabaseConfig config;
    private final Logger logger;

    private final Map<UUID, PlayerData> loaded = new ConcurrentHashMap<>();
    @Getter
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
        long flushPeriod = config.getFlushIntervalSeconds();
        scheduler.scheduleWithFixedDelay(this::flushSafely, flushPeriod, flushPeriod, TimeUnit.SECONDS);
        long unloadPeriod = Math.max(MIN_UNLOAD_PERIOD_SECONDS, config.getUnloadDelaySeconds());
        scheduler.scheduleWithFixedDelay(this::evictOffline, unloadPeriod, unloadPeriod, TimeUnit.SECONDS);
        cache.setKitInvalidationListener(this::forgetKitLocally);
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
        PlayerData existing = get(uuid);
        if (existing != null) {
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
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
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
            workers.execute(() -> flushPlayer(data));
        }
    }

    private void evictOffline() {
        long threshold = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(config.getUnloadDelaySeconds());
        loaded.values().removeIf(data -> {
            if (data.isOnline() || data.getLastAccess() > threshold) {
                return false;
            }
            flushPlayer(data);
            return true;
        });
    }

    private void flushPlayer(PlayerData data) {
        PendingWrite.drain(data).ifPresent(pending -> write(List.of(pending)));
    }

    private void flushSafely() {
        try {
            flush();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при сохранении данных игроков", e);
        }
    }

    public void flush() {
        List<PendingWrite> pending = new ArrayList<>();
        for (PlayerData data : loaded.values()) {
            PendingWrite.drain(data).ifPresent(pending::add);
        }
        write(pending);
    }

    private void write(List<PendingWrite> pending) {
        if (pending.isEmpty()) {
            return;
        }
        List<DataEntry> cooldownBatch = new ArrayList<>();
        List<DataEntry> claimBatch = new ArrayList<>();
        long fallback = System.currentTimeMillis();
        for (PendingWrite write : pending) {
            write.appendEntries(cooldownBatch, claimBatch, fallback);
        }
        try {
            storage.writeCooldowns(cooldownBatch);
            storage.writeClaims(claimBatch);
            writes.addAndGet(cooldownBatch.size() + claimBatch.size());
            pending.forEach(write -> cache.put(write.getData().toRecord()));
        } catch (Exception e) {
            pending.forEach(PendingWrite::restore);
            logger.log(Level.SEVERE, "Не удалось записать данные игроков, повтор при следующем сбросе", e);
        }
    }

    public void onKitRemoved(String kitKey) {
        forgetKitLocally(kitKey);
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

    private void forgetKitLocally(String kitKey) {
        loaded.values().forEach(data -> data.forgetKit(kitKey));
    }

    public void shutdown() {
        scheduler.shutdownNow();
        workers.shutdown();
        try {
            if (!workers.awaitTermination(15L, TimeUnit.SECONDS)) {
                workers.shutdownNow();
            }
        } catch (InterruptedException e) {
            workers.shutdownNow();
            Thread.currentThread().interrupt();
        }
        flushSafely();
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
        long lookups = getCacheLookups();
        return lookups == 0L ? 0L : getCacheHits() * 100L / lookups;
    }

    public long getWrites() {
        return writes.get();
    }

    @RequiredArgsConstructor
    private static final class PendingWrite {

        @Getter
        private final PlayerData data;
        private final List<String> cooldownKeys;
        private final List<String> claimKeys;

        static Optional<PendingWrite> drain(PlayerData data) {
            if (!data.isDirty()) {
                return Optional.empty();
            }
            List<String> cooldownKeys = data.drainDirtyCooldowns();
            List<String> claimKeys = data.drainDirtyClaims();
            if (cooldownKeys.isEmpty() && claimKeys.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new PendingWrite(data, cooldownKeys, claimKeys));
        }

        void appendEntries(List<DataEntry> cooldownBatch, List<DataEntry> claimBatch, long fallback) {
            append(cooldownBatch, cooldownKeys, fallback);
            append(claimBatch, claimKeys, fallback);
        }

        void restore() {
            data.restoreDirtyCooldowns(cooldownKeys);
            data.restoreDirtyClaims(claimKeys);
        }

        private void append(List<DataEntry> batch, List<String> keys, long fallback) {
            for (String key : keys) {
                long stamp = data.getCooldown(key);
                batch.add(new DataEntry(data.getUuid(), key, stamp == 0L ? fallback : stamp));
            }
        }
    }
}
