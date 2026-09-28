package org.nezxenka.strictkits.command.argument;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class CooldownArgument {

    public enum Status {
        VALID,
        NOT_A_NUMBER,
        NEGATIVE,
        TOO_LARGE
    }

    private final Status status;
    private final long seconds;

    public static CooldownArgument parse(String raw, long maxSeconds) {
        long seconds;
        try {
            seconds = Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return new CooldownArgument(Status.NOT_A_NUMBER, 0L);
        }
        if (seconds < 0L) {
            return new CooldownArgument(Status.NEGATIVE, seconds);
        }
        if (seconds > maxSeconds) {
            return new CooldownArgument(Status.TOO_LARGE, seconds);
        }
        return new CooldownArgument(Status.VALID, seconds);
    }
}
