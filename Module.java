package dev.bloom.module;

import dev.bloom.Config;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/** A toggleable feature with settings. */
public abstract class Module {
    public final String id, name, category, description;
    public boolean enabled;
    public final List<Setting> settings = new ArrayList<>();

    protected Module(String id, String name, String category, String description, boolean defaultOn) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.enabled = Config.getBool("mod." + id + ".enabled", defaultOn);
    }

    public void setEnabled(boolean on) {
        enabled = on;
        Config.put("mod." + id + ".enabled", Boolean.toString(on));
    }

    public void toggle() { setEnabled(!enabled); }

    /** Runs every client tick while enabled. */
    public void tick(Minecraft mc) { }

    protected <T extends Setting> T add(T s) {
        s.key = "mod." + id + "." + s.id;
        String v = Config.get(s.key, null);
        if (v != null) {
            try { s.decode(v); } catch (RuntimeException ignored) { }
        }
        settings.add(s);
        return s;
    }
}
