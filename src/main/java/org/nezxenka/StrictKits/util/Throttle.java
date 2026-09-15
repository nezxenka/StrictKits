package org.nezxenka.StrictKits.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Throttle {

    private static final int MAX_ENTRIES = 4096;
    private static final long STALE_MULTIPLIER = 20L;

    private final Map<UUID, Long> stamps = new ConcurrentHashMap<>();

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
            stamps.values().removeIf(stamp -> now - stamp > intervalMillis * STALE_MULTIPLIER);
        }
        return true;
    }
}
