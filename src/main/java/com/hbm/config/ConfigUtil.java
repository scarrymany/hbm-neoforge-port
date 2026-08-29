package com.hbm.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * ModConfigSpec has no first-class {@code Map<K, V>} config value type. Tables that CE stored as
 * {@code HashMap<Integer/String, Integer/Float>} are stored here as "key:value" (or "key=value")
 * string lists instead, mirroring CE's own {@code CommonConfig.createConfigHashMap} convention.
 * <p>
 * Callers parse the raw list into a map on demand via these helpers rather than caching it at
 * config-load time, since these tables back rarely-read lookups (mob spawn setup, world-gen
 * tables) rather than a hot path, and on-demand parsing avoids needing a
 * {@code ModConfigEvent.Loading}/{@code Reloading} listener just to keep a cache warm.
 */
public final class ConfigUtil {

    private ConfigUtil() {}

    public static Map<String, Integer> toIntMap(List<? extends String> entries, String delimiter) {
        Map<String, Integer> result = new HashMap<>();
        for (String entry : entries) {
            put(result, entry, delimiter, Integer::parseInt);
        }
        return result;
    }

    public static Map<String, Float> toFloatMap(List<? extends String> entries, String delimiter) {
        Map<String, Float> result = new HashMap<>();
        for (String entry : entries) {
            put(result, entry, delimiter, Float::parseFloat);
        }
        return result;
    }

    private static <V> void put(Map<String, V> target, String entry, String delimiter, Function<String, V> valueParser) {
        if (entry == null) return;
        int split = entry.indexOf(delimiter);
        if (split <= 0 || split == entry.length() - delimiter.length()) return;
        String key = entry.substring(0, split).trim();
        String rawValue = entry.substring(split + delimiter.length()).trim();
        try {
            target.put(key, valueParser.apply(rawValue));
        } catch (NumberFormatException ignored) {
            // malformed entry, skip it rather than failing the whole config load
        }
    }
}
