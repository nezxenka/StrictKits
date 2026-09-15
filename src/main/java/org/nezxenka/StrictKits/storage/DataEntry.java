package org.nezxenka.StrictKits.storage;

import lombok.Value;

import java.util.UUID;

@Value
public class DataEntry {

    UUID uuid;
    String kit;
    long timestamp;
}
