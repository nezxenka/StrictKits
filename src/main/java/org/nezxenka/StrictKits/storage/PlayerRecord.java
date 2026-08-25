package org.nezxenka.StrictKits.storage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlayerRecord {

    private final UUID uuid;
    private final Map<String, Long> cooldowns;
    private final Set<String> claims;

    public PlayerRecord(UUID uuid, Map<String, Long> cooldowns, Set<String> claims) {
        this.uuid = uuid;
        this.cooldowns = cooldowns;
        this.claims = claims;
    }

    public static PlayerRecord empty(UUID uuid) {
        return new PlayerRecord(uuid, new HashMap<>(4), new HashSet<>(4));
    }

    public UUID getUuid() {
        return uuid;
    }

    public Map<String, Long> getCooldowns() {
        return cooldowns;
    }

    public Set<String> getClaims() {
        return claims;
    }
}
