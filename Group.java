package dev.bloom;

/** An optional feature group. Loaded by name at startup; if its class is missing or fails, the rest of the mod still works. */
public interface Group {
    void init();
}
