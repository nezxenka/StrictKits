package org.nezxenka.StrictKits.player;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.nezxenka.StrictKits.storage.PlayerRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public final class PlayerData {

    @Getter
    private final UUID uuid;
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>(8);
    private final Set<String> claims = ConcurrentHashMap.newKeySet(8);
    private final Set<String> dirtyCooldowns = ConcurrentHashMap.newKeySet(4);
    private final Set<String> dirtyClaims = ConcurrentHashMap.newKeySet(4);

    @Getter
    private volatile long lastAccess = System.currentTimeMillis();
    @Getter
    private volatile boolean online;

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
        dirtyCooldowns.add(kitKey);
        touch();
    }

    public boolean hasClaim(String kitKey) {
        return claims.contains(kitKey);
    }

    public void addClaim(String kitKey, long timestamp) {
        if (claims.add(kitKey)) {
            dirtyClaims.add(kitKey);
        }
        cooldowns.put(kitKey, timestamp);
        touch();
    }

    public void forgetKit(String kitKey) {
        cooldowns.remove(kitKey);
        claims.remove(kitKey);
        dirtyCooldowns.remove(kitKey);
        dirtyClaims.remove(kitKey);
    }

    public boolean isDirty() {
        return !dirtyCooldowns.isEmpty() || !dirtyClaims.isEmpty();
    }

    public List<String> drainDirtyCooldowns() {
        return drain(dirtyCooldowns);
    }

    public List<String> drainDirtyClaims() {
        return drain(dirtyClaims);
    }

    public void restoreDirtyCooldowns(List<String> keys) {
        dirtyCooldowns.addAll(keys);
    }

    public void restoreDirtyClaims(List<String> keys) {
        dirtyClaims.addAll(keys);
    }

    public PlayerRecord toRecord() {
        return new PlayerRecord(uuid, new HashMap<>(cooldowns), new HashSet<>(claims));
    }

    public void touch() {
        lastAccess = System.currentTimeMillis();
    }

    public void setOnline(boolean online) {
        this.online = online;
        touch();
    }

    private static List<String> drain(Set<String> source) {
        List<String> drained = new ArrayList<>(source.size());
        for (String key : source) {
            if (source.remove(key)) {
                drained.add(key);
            }
        }
        return drained;
    }
}
