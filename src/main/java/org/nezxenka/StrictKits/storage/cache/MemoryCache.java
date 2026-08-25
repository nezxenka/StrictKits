package org.nezxenka.StrictKits.storage.cache;

import org.nezxenka.StrictKits.storage.PlayerRecord;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class MemoryCache implements CacheProvider {

    private static final int MAX_ENTRIES = 20000;

    private final ConcurrentHashMap<UUID, Holder> entries = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public MemoryCache(long ttlMillis) {
        this.ttlMillis = ttlMillis;
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
        if (entries.size() >= MAX_ENTRIES) {
            evict();
        }
        entries.put(record.getUuid(), new Holder(RecordCodec.encode(record), System.currentTimeMillis()));
    }

    @Override
    public void invalidate(UUID uuid) {
        entries.remove(uuid);
    }

    @Override
    public void invalidateKit(String kit) {
        entries.clear();
    }

    @Override
    public void setRemoteInvalidationListener(Consumer<UUID> playerListener, Consumer<String> kitListener) {
    }

    private void evict() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Holder>> iterator = entries.entrySet().iterator();
        int removed = 0;
        while (iterator.hasNext()) {
            Map.Entry<UUID, Holder> entry = iterator.next();
            if (now - entry.getValue().stamp > ttlMillis) {
                iterator.remove();
                removed++;
            }
        }
        if (removed == 0) {
            entries.clear();
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
