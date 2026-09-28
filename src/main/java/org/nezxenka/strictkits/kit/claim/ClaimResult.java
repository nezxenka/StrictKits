package org.nezxenka.strictkits.kit.claim;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.EnumMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClaimResult {

    public enum Status {
        GRANTED,
        DATA_NOT_LOADED,
        NO_PERMISSION,
        ALREADY_CLAIMED,
        ON_COOLDOWN,
        EMPTY
    }

    private static final Map<Status, ClaimResult> SIMPLE = new EnumMap<>(Status.class);

    static {
        for (Status status : Status.values()) {
            SIMPLE.put(status, new ClaimResult(status, 0L));
        }
    }

    private final Status status;
    private final long remainingMillis;

    public static ClaimResult of(Status status) {
        return SIMPLE.get(status);
    }

    public static ClaimResult granted() {
        return of(Status.GRANTED);
    }

    public static ClaimResult onCooldown(long remainingMillis) {
        return new ClaimResult(Status.ON_COOLDOWN, remainingMillis);
    }

    public boolean isGranted() {
        return status == Status.GRANTED;
    }
}
