package org.nezxenka.StrictKits.storage;

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
