package dev.bloom.module;

import dev.bloom.lyrics.LyricsModule;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();

    private ModuleManager() { }

    public static void init() {
        MODULES.clear();
        MODULES.add(new Modules.Watermark());
        MODULES.add(new Modules.Fps());
        MODULES.add(new Modules.Cps());
        MODULES.add(new Modules.Keystrokes());
        MODULES.add(new Modules.Coordinates());
        MODULES.add(new Modules.Direction());
        MODULES.add(new Modules.Clock());
        MODULES.add(new Modules.Playtime());
        MODULES.add(new Modules.Memory());
        MODULES.add(new Modules.Speed());
        MODULES.add(new Modules.Ping());
        MODULES.add(new Modules.ServerAddress());
        MODULES.add(new Modules.HeldItem());
        MODULES.add(new Modules.PotionStatus());
        MODULES.add(new Modules.ArmorStatus());
        MODULES.add(new Modules.ToggleSprint());
        MODULES.add(new LyricsModule());
    }

    public static List<Module> all() { return Collections.unmodifiableList(MODULES); }

    public static void tick(Minecraft mc) {
        for (Module m : MODULES) if (m.enabled) m.tick(mc);
    }
}
