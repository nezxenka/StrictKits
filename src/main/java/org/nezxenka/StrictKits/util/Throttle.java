package org.nezxenka.StrictKits.util;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Throttle {

    private static final int MAX_ENTRIES = 4096;

    private final ConcurrentHashMap<UUID, Long> stamps = new ConcurrentHashMap<>();

    public boolean allow(UUID uuid, long intervalMillis) {
        if (intervalMillis <= 0L) {
            return true;
        }
        long now = System.currentTimeMillis();
        Long previous = stamps.get(uuid);
        if (previous != null && now - previous < intervalMillis) {
            return false;
        }
        stamps.put(uuid, now);
        if (stamps.size() > MAX_ENTRIES) {
            prune(now, intervalMillis);
        }
        return true;
    }

    private void prune(long now, long intervalMillis) {
        Iterator<Map.Entry<UUID, Long>> iterator = stamps.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue() > intervalMillis * 20L) {
                iterator.remove();
            }
        }
    }
}
