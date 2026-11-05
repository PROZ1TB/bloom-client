package dev.bloom;

import dev.bloom.module.HudModule;
import dev.bloom.module.Module;
import dev.bloom.module.ModuleManager;
import dev.bloom.ui.Input;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Draws every enabled HUD module every frame. */
public final class HudRenderer {
    public static int fps;
    private static int frames;
    private static long windowStart = System.nanoTime();

    private HudRenderer() { }

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        Input.poll();
        frames++;
        long now = System.nanoTime();
        if (now - windowStart >= 1_000_000_000L) {
            fps = (int) Math.round(frames * 1.0e9 / (now - windowStart));
            frames = 0;
            windowStart = now;
        }
        if (mc.options.hideGui) return;
        int gw = mc.getWindow().getGuiScaledWidth(), gh = mc.getWindow().getGuiScaledHeight();
        for (Module m : ModuleManager.all()) {
            if (m.enabled && m instanceof HudModule h) h.renderAt(g, mc, gw, gh);
        }
    }
}
