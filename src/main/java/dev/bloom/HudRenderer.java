package dev.bloom;

import dev.bloom.module.HudModule;
import dev.bloom.module.Module;
import dev.bloom.module.ModuleManager;
import dev.bloom.module.Overlay;
import dev.bloom.ui.Input;
import dev.bloom.ui.Ui;
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
        Ui.beginFrame();
        frames++;
        long now = System.nanoTime();
        if (now - windowStart >= 1_000_000_000L) {
            fps = (int) Math.round(frames * 1.0e9 / (now - windowStart));
            frames = 0;
            windowStart = now;
        }
        int gw = mc.getWindow().getGuiScaledWidth(), gh = mc.getWindow().getGuiScaledHeight();
        for (Module m : ModuleManager.all()) {
            if (m.enabled && m instanceof Overlay o) {
                try {
                    o.renderOverlay(g, mc, gw, gh);
                } catch (RuntimeException e) {
                    m.setEnabled(false);
                    System.out.println("[Bloom] " + m.name + " turned itself off after an error: " + e);
                }
            }
        }
        if (mc.options.hideGui) return;
        for (Module m : ModuleManager.all()) {
            if (m.enabled && m instanceof HudModule h) h.renderAt(g, mc, gw, gh);
        }
    }
}
