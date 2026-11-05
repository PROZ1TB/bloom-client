package dev.bloom.risky.sky;

import dev.bloom.Group;
import dev.bloom.module.ModuleManager;

public final class SkyGroup implements Group {
    @Override
    public void init() {
        ModuleManager.register(new SkyOverlayModule());
    }
}
