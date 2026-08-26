package org.nezxenka.StrictKits.storage;

import java.util.List;
import java.util.UUID;

public interface StorageProvider {

    void initialize() throws Exception;

    void shutdown();

    String name();

    PlayerRecord load(UUID uuid) throws Exception;

    void writeCooldowns(List<DataEntry> entries) throws Exception;

    void writeClaims(List<DataEntry> entries) throws Exception;

    void deleteKit(String kit) throws Exception;

    int purgeCooldowns(String kit, long cutoff) throws Exception;
}
