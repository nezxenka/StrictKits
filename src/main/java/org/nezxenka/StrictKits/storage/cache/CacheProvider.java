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

    void invalidateKit(String kit);

    void setKitInvalidationListener(Consumer<String> kitListener);
}
