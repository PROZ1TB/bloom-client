package dev.bloom.risky.menu;

import dev.bloom.Config;
import dev.bloom.Group;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;

/** Swaps the vanilla title screen for the Bloom main menu. */
public final class MenuGroup implements Group {
    @Override
    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (BloomTitleScreen.disabled || !Config.getBool("menu.enabled", true)) return;
            if (mc.screen instanceof TitleScreen) mc.setScreen(new BloomTitleScreen());
        });
    }
}
