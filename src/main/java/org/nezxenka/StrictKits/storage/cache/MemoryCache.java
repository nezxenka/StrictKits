package org.nezxenka.StrictKits.storage.cache;

import org.nezxenka.StrictKits.storage.PlayerRecord;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class MemoryCache implements CacheProvider {

    private static final int MAX_ENTRIES = 20000;
    private static final int RETAIN_ENTRIES = MAX_ENTRIES * 3 / 4;

    private final ConcurrentHashMap<UUID, Holder> entries = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public MemoryCache(long ttlMillis) {
        this.ttlMillis = Math.max(1000L, ttlMillis);
    }

    @Override
    public void initialize() {
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
        Holder holder = entries.get(uuid);
        if (holder == null) {
            return null;
        }
        if (System.currentTimeMillis() - holder.stamp > ttlMillis) {
            entries.remove(uuid, holder);
            return null;
        }
        return RecordCodec.decode(uuid, holder.payload);
    }

    @Override
    public void put(PlayerRecord record) {
        String payload = RecordCodec.encode(record);
        if (payload == null) {
            return;
        }
        if (entries.size() >= MAX_ENTRIES) {
            evict();
        }
        entries.put(record.getUuid(), new Holder(payload, System.currentTimeMillis()));
    }

    @Override
    public void invalidateKit(String kit) {
        entries.clear();
    }

    @Override
    public void setKitInvalidationListener(Consumer<String> kitListener) {
    }

    private void evict() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Holder>> iterator = entries.entrySet().iterator();
        int removed = 0;
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue().stamp > ttlMillis) {
                iterator.remove();
                removed++;
            }
        }
        if (removed > 0) {
            return;
        }
        int excess = entries.size() - RETAIN_ENTRIES;
        if (excess <= 0) {
            return;
        }
        List<Map.Entry<UUID, Holder>> sorted = new ArrayList<>(entries.entrySet());
        sorted.sort(Comparator.comparingLong(entry -> entry.getValue().stamp));
        int limit = Math.min(excess, sorted.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<UUID, Holder> entry = sorted.get(i);
            entries.remove(entry.getKey(), entry.getValue());
        }
    }

    private static final class Holder {
        private final String payload;
        private final long stamp;

        private Holder(String payload, long stamp) {
            this.payload = payload;
            this.stamp = stamp;
        }
    }
}
