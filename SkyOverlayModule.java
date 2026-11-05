package dev.bloom.risky.sky;

import dev.bloom.Theme;
import dev.bloom.module.Module;
import dev.bloom.module.Overlay;
import dev.bloom.module.Setting;
import dev.bloom.visual.SkyArt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Custom Sky: paints an animated sky over the part of the screen above the horizon. It follows
 * your camera, so the aurora and stars stay in place in the world as you look around.
 */
public final class SkyOverlayModule extends Module implements Overlay {
    private final Setting.Mode style = add(new Setting.Mode("style", "Style", 0, SkyArt.STYLES));
    private final Setting.Num intensity = add(new Setting.Num("intensity", "Intensity", 0.2, 1.5, 0.05, 0.8));
    private final Setting.Num speed = add(new Setting.Num("speed", "Speed", 0.2, 3.0, 0.1, 1.0));
    private final Setting.Num fov = add(new Setting.Num("fov", "Your FOV", 30, 130, 1, 70));
    private final Setting.Bool stars = add(new Setting.Bool("stars", "Stars", true));
    private final Setting.Bool glow = add(new Setting.Bool("glow", "Horizon Glow", true));
    private final Setting.Bool veil = add(new Setting.Bool("veil", "Darken Sky", true));
    private final Setting.Bool outdoors = add(new Setting.Bool("outdoors", "Only Outdoors", true));

    private double skyTime;
    private long last = System.nanoTime();

    public SkyOverlayModule() {
        super("sky", "Custom Sky", "Visual", "Animated sky (aurora, nebula, swirl, golden, sunset). Set Your FOV to match the game.", true);
    }

    @Override
    public void renderOverlay(GuiGraphics g, Minecraft mc, int gw, int gh) {
        long now = System.nanoTime();
        double dt = Math.min(0.1, (now - last) / 1.0e9);
        last = now;
        skyTime += dt * speed.value;
        if (mc.player == null || mc.level == null) return;
        if (outdoors.value && !mc.level.canSeeSky(mc.player.blockPosition())) return;
        SkyArt.draw(g, gw, gh, mc.player.getYRot(), mc.player.getXRot(), fov.value, style.index,
                intensity.value, skyTime, stars.value, glow.value, veil.value, Theme.accent);
    }
}
