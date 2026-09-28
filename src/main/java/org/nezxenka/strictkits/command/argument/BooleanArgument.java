package org.nezxenka.strictkits.command.argument;

import lombok.experimental.UtilityClass;
import org.nezxenka.strictkits.util.command.Completions;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@UtilityClass
public class BooleanArgument {

    public static final List<String> VALUES = List.of("true", "false");

    public static Optional<Boolean> parse(String raw) {
        String value = raw.toLowerCase(Locale.ROOT);
        return VALUES.contains(value) ? Optional.of(Boolean.parseBoolean(value)) : Optional.empty();
    }

    public static List<String> complete(String prefix) {
        return Completions.filter(VALUES, prefix);
    }
}
