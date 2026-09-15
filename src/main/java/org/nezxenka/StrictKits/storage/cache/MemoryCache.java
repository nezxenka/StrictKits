package org.nezxenka.StrictKits.storage.cache;

import lombok.Value;
import org.nezxenka.StrictKits.storage.PlayerRecord;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MemoryCache implements CacheProvider {

    private static final int MAX_ENTRIES = 20000;
    private static final int RETAIN_ENTRIES = MAX_ENTRIES * 3 / 4;

    private final Map<UUID, CachedRecord> entries = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public MemoryCache(long ttlMillis) {
        this.ttlMillis = Math.max(1000L, ttlMillis);
    }

    @Override
    public void shutdown() {
        entries.clear();
    }

    @Override
    public String name() {
        return "Memory";
    }

    @Override
    public PlayerRecord get(UUID uuid) {
        CachedRecord cached = entries.get(uuid);
        if (cached == null) {
            return null;
        }
        if (isExpired(cached, System.currentTimeMillis())) {
            entries.remove(uuid, cached);
            return null;
        }
        return cached.getRecord();
    }

    @Override
    public void put(PlayerRecord record) {
        if (entries.size() >= MAX_ENTRIES) {
            evict();
        }
        PlayerRecord snapshot = new PlayerRecord(
                record.getUuid(), Map.copyOf(record.getCooldowns()), Set.copyOf(record.getClaims()));
        entries.put(record.getUuid(), new CachedRecord(snapshot, System.currentTimeMillis()));
    }

    @Override
    public void invalidateKit(String kit) {
        entries.clear();
    }

    private boolean isExpired(CachedRecord cached, long now) {
        return now - cached.getStamp() > ttlMillis;
    }

    private void evict() {
        long now = System.currentTimeMillis();
        if (entries.values().removeIf(cached -> isExpired(cached, now))) {
            return;
        }
        int excess = entries.size() - RETAIN_ENTRIES;
        if (excess <= 0) {
            return;
        }
        entries.entrySet().stream()
                .sorted(Comparator.comparingLong(entry -> entry.getValue().getStamp()))
                .limit(excess)
                .toList()
                .forEach(entry -> entries.remove(entry.getKey(), entry.getValue()));
    }

    @Value
    private static class CachedRecord {

        PlayerRecord record;
        long stamp;
    }
}
