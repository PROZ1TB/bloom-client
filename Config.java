package dev.bloom;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tiny key=value config (no JSON library needed). Saved to config/bloom/bloom.properties. */
public final class Config {
    private static final Map<String, String> MAP = new LinkedHashMap<>();
    private static boolean dirty;
    private static long lastSave;

    private Config() { }

    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("bloom");
    }

    public static void load() {
        MAP.clear();
        Path f = dir().resolve("bloom.properties");
        if (!Files.exists(f)) return;
        try {
            List<String> lines = Files.readAllLines(f, StandardCharsets.UTF_8);
            for (String l : lines) {
                int i = l.indexOf('=');
                if (i <= 0 || l.startsWith("#")) continue;
                MAP.put(l.substring(0, i).trim(), l.substring(i + 1).trim());
            }
        } catch (IOException ignored) { }
    }

    public static void save() {
        dirty = false;
        lastSave = System.currentTimeMillis();
        try {
            Files.createDirectories(dir());
            StringBuilder sb = new StringBuilder("# Bloom Client settings\n");
            for (Map.Entry<String, String> e : MAP.entrySet()) {
                sb.append(e.getKey()).append('=').append(e.getValue()).append('\n');
            }
            Files.writeString(dir().resolve("bloom.properties"), sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) { }
    }

    /** Called every client tick; writes the file at most once a second after a change. */
    public static void tick() {
        if (dirty && System.currentTimeMillis() - lastSave > 1000) save();
    }

    public static String get(String key, String def) {
        String v = MAP.get(key);
        return v == null ? def : v;
    }

    public static boolean getBool(String key, boolean def) {
        String v = MAP.get(key);
        return v == null ? def : Boolean.parseBoolean(v);
    }

    public static double getDouble(String key, double def) {
        String v = MAP.get(key);
        if (v == null) return def;
        try { return Double.parseDouble(v); } catch (NumberFormatException e) { return def; }
    }

    public static void put(String key, String value) {
        if (value.equals(MAP.get(key))) return;
        MAP.put(key, value);
        dirty = true;
    }
}
