package org.nezxenka.strictkits.player.session;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerSessions {

    private final Set<UUID> online = ConcurrentHashMap.newKeySet();

    public void markOnline(UUID uuid) {
        online.add(uuid);
    }

    public void markOffline(UUID uuid) {
        online.remove(uuid);
    }

    public boolean isOnline(UUID uuid) {
        return online.contains(uuid);
    }

    public void clear() {
        online.clear();
    }
}
