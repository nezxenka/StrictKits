package org.nezxenka.StrictKits.storage.cache;

import org.nezxenka.StrictKits.storage.PlayerRecord;

import java.util.UUID;
import java.util.function.Consumer;

public interface CacheProvider {

    void initialize() throws Exception;

    void shutdown();

    String name();

    PlayerRecord get(UUID uuid);

    void put(PlayerRecord record);

    void invalidate(UUID uuid);

    void invalidateKit(String kit);

    void setRemoteInvalidationListener(Consumer<UUID> playerListener, Consumer<String> kitListener);
}
