package dev.bloom.module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** A module that draws full-screen effects behind the HUD boxes (for example the sky overlay). */
public interface Overlay {
    void renderOverlay(GuiGraphics g, Minecraft mc, int gw, int gh);
}
