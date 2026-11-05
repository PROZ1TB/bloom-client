package dev.bloom.risky.glow;

import dev.bloom.Group;
import dev.bloom.module.ModuleManager;

public final class GlowGroup implements Group {
    @Override
    public void init() {
        ModuleManager.register(new PlayerGlowModule());
    }
}
