package org.nezxenka.StrictKits.util;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Throttle {

    private final ConcurrentHashMap<UUID, Long> stamps = new ConcurrentHashMap<>();
    private final long intervalMillis;

    public Throttle(long intervalMillis) {
        this.intervalMillis = intervalMillis;
    }

    public boolean allow(UUID uuid) {
        if (intervalMillis <= 0L) {
            return true;
        }
        long now = System.currentTimeMillis();
        Long previous = stamps.get(uuid);
        if (previous != null && now - previous < intervalMillis) {
            return false;
        }
        stamps.put(uuid, now);
        if (stamps.size() > 4096) {
            prune(now);
        }
        return true;
    }

    public void forget(UUID uuid) {
        stamps.remove(uuid);
    }

    private void prune(long now) {
        Iterator<Map.Entry<UUID, Long>> iterator = stamps.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue() > intervalMillis * 20L) {
                iterator.remove();
            }
        }
    }
}
