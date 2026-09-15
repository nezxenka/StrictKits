package org.nezxenka.StrictKits.util;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@UtilityClass
public class Completions {

    public static List<String> filter(Collection<String> source, String prefix) {
        List<String> matches = new ArrayList<>();
        collect(matches, source, prefix);
        return matches;
    }

    public static void collect(List<String> target, Collection<String> source, String prefix) {
        for (String value : source) {
            if (value.regionMatches(true, 0, prefix, 0, prefix.length())) {
                target.add(value);
            }
        }
    }
}
