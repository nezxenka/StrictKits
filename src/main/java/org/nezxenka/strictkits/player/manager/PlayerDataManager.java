package org.nezxenka.strictkits.player.manager;

import org.nezxenka.strictkits.player.data.PlayerData;
import org.nezxenka.strictkits.player.persistence.PlayerDataWriter;
import org.nezxenka.strictkits.player.session.PlayerSessions;
import org.nezxenka.strictkits.storage.api.StorageProvider;
import org.nezxenka.strictkits.util.concurrent.DaemonThreadFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PlayerDataManager {

    private static final long RETRY_INTERVAL_SECONDS = 30L;
    private static final long LOGIN_GRACE_MILLIS = TimeUnit.MINUTES.toMillis(1L);

    private final StorageProvider storage;
    private final PlayerDataWriter writer;
    private final Logger logger;

    private final Map<UUID, PlayerData> loaded = new ConcurrentHashMap<>();
    private final PlayerSessions sessions = new PlayerSessions();
    private final Set<UUID> loading = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService io = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory("StrictKits-IO"));

    public PlayerDataManager(StorageProvider storage, Logger logger) {
        this.storage = storage;
        this.writer = new PlayerDataWriter(storage, logger);
        this.logger = logger;
    }

    public void start() {
        io.scheduleWithFixedDelay(this::maintain, RETRY_INTERVAL_SECONDS, RETRY_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public PlayerData get(UUID uuid) {
        return loaded.get(uuid);
    }

    public PlayerData getOrRequest(UUID uuid) {
        PlayerData data = loaded.get(uuid);
        if (data == null) {
            requestLoad(uuid);
        }
        return data;
    }

    public boolean isLoaded(UUID uuid) {
        return loaded.containsKey(uuid);
    }

    public boolean load(UUID uuid) {
        PlayerData current = loaded.get(uuid);
        if (current != null && isAuthoritative(current)) {
            return true;
        }
        try {
            PlayerData fresh = PlayerData.from(storage.load(uuid));
            loaded.merge(uuid, fresh, (existing, loadedNow) -> isAuthoritative(existing) ? existing : loadedNow);
            return true;
        } catch (Exception e) {
            if (current != null) {
                loaded.remove(uuid, current);
            }
            logger.log(Level.SEVERE, "Не удалось загрузить данные игрока " + uuid + ", киты для него недоступны", e);
            return false;
        }
    }

    public void requestLoad(UUID uuid) {
        requestLoad(uuid, () -> {
        });
    }

    public void requestLoad(UUID uuid, Runnable onLoaded) {
        if (!loading.add(uuid)) {
            return;
        }
        io.execute(() -> {
            try {
                if (load(uuid)) {
                    onLoaded.run();
                }
            } finally {
                loading.remove(uuid);
            }
        });
    }

    public void runAsync(Runnable task) {
        io.execute(task);
    }

    public void markOnline(UUID uuid) {
        sessions.markOnline(uuid);
    }

    public void markOffline(UUID uuid) {
        sessions.markOffline(uuid);
        PlayerData data = loaded.get(uuid);
        if (data != null) {
            unloadIfIdle(data);
        }
    }

    public void recordCooldown(PlayerData data, String kitKey, long timestamp) {
        data.setCooldown(kitKey, timestamp);
        saveAsync(data);
    }

    public void recordClaim(PlayerData data, String kitKey, long timestamp) {
        data.addClaim(kitKey, timestamp);
        saveAsync(data);
    }

    public void onKitRemoved(String kitKey) {
        loaded.values().forEach(data -> data.forgetKit(kitKey));
        io.execute(() -> {
            try {
                storage.deleteKit(kitKey);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Не удалось удалить данные кита " + kitKey, e);
            }
        });
    }

    public void purgeExpired(String kitKey, long cooldownMillis) {
        if (cooldownMillis <= 0L) {
            return;
        }
        io.execute(() -> {
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

    public void shutdown() {
        io.shutdown();
        try {
            if (!io.awaitTermination(15L, TimeUnit.SECONDS)) {
                io.shutdownNow();
            }
        } catch (InterruptedException e) {
            io.shutdownNow();
            Thread.currentThread().interrupt();
        }
        save(loaded.values());
        long unsaved = loaded.values().stream().filter(PlayerData::hasUnsaved).count();
        if (unsaved > 0L) {
            logger.severe("Не удалось сохранить данные игроков при выключении: " + unsaved);
        }
        loaded.clear();
        sessions.clear();
    }

    public int getLoadedCount() {
        return loaded.size();
    }

    public long getWrites() {
        return writer.getWritten();
    }

    private void saveAsync(PlayerData data) {
        if (!writer.isFailing()) {
            io.execute(() -> save(List.of(data)));
        }
    }

    private void save(Collection<PlayerData> candidates) {
        writer.save(candidates).forEach(this::unloadIfIdle);
    }

    private void maintain() {
        try {
            save(loaded.values());
            long threshold = System.currentTimeMillis() - LOGIN_GRACE_MILLIS;
            loaded.values().removeIf(data -> !isAuthoritative(data) && data.getLoadedAt() < threshold);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка обслуживания данных игроков", e);
        }
    }

    private boolean isAuthoritative(PlayerData data) {
        return sessions.isOnline(data.getUuid()) || data.hasUnsaved();
    }

    private void unloadIfIdle(PlayerData data) {
        if (!isAuthoritative(data)) {
            loaded.remove(data.getUuid(), data);
        }
    }
}
