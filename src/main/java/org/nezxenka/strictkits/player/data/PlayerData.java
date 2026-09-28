package org.nezxenka.strictkits.player.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.nezxenka.strictkits.storage.model.PlayerRecord;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public final class PlayerData {

    @Getter
    private final UUID uuid;
    @Getter
    private final long loadedAt = System.currentTimeMillis();
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>(8);
    private final Set<String> claims = ConcurrentHashMap.newKeySet(8);
    private final Map<String, Long> unsavedCooldowns = new ConcurrentHashMap<>(4);
    private final Map<String, Long> unsavedClaims = new ConcurrentHashMap<>(4);

    public static PlayerData from(PlayerRecord record) {
        PlayerData data = new PlayerData(record.getUuid());
        data.cooldowns.putAll(record.getCooldowns());
        data.claims.addAll(record.getClaims());
        return data;
    }

    public long getCooldown(String kitKey) {
        return cooldowns.getOrDefault(kitKey, 0L);
    }

    public void setCooldown(String kitKey, long timestamp) {
        cooldowns.put(kitKey, timestamp);
        unsavedCooldowns.put(kitKey, timestamp);
    }

    public boolean hasClaim(String kitKey) {
        return claims.contains(kitKey);
    }

    public void addClaim(String kitKey, long timestamp) {
        if (claims.add(kitKey)) {
            unsavedClaims.put(kitKey, timestamp);
        }
    }

    public void forgetKit(String kitKey) {
        cooldowns.remove(kitKey);
        claims.remove(kitKey);
        unsavedCooldowns.remove(kitKey);
        unsavedClaims.remove(kitKey);
    }

    public boolean hasUnsaved() {
        return !unsavedCooldowns.isEmpty() || !unsavedClaims.isEmpty();
    }

    public Map<String, Long> unsavedCooldowns() {
        return Map.copyOf(unsavedCooldowns);
    }

    public Map<String, Long> unsavedClaims() {
        return Map.copyOf(unsavedClaims);
    }

    public void markSaved(Map<String, Long> savedCooldowns, Map<String, Long> savedClaims) {
        savedCooldowns.forEach((kitKey, timestamp) -> unsavedCooldowns.remove(kitKey, timestamp));
        savedClaims.forEach((kitKey, timestamp) -> unsavedClaims.remove(kitKey, timestamp));
    }
}
