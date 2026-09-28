package org.nezxenka.strictkits.storage.model;

import lombok.Value;

import java.util.UUID;

@Value
public class DataEntry {

    UUID uuid;
    String kit;
    long timestamp;
}
