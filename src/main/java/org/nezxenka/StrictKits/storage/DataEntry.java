package org.nezxenka.StrictKits.storage;

import java.util.UUID;

public final class DataEntry {

    private final UUID uuid;
    private final String kit;
    private final long timestamp;

    public DataEntry(UUID uuid, String kit, long timestamp) {
        this.uuid = uuid;
        this.kit = kit;
        this.timestamp = timestamp;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getKit() {
        return kit;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
