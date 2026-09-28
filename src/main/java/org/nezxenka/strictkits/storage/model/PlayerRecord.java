package org.nezxenka.strictkits.storage.model;

import lombok.Value;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Value
public class PlayerRecord {

    UUID uuid;
    Map<String, Long> cooldowns;
    Set<String> claims;
}
