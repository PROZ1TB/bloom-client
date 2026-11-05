package dev.bloom.screen;

import dev.bloom.Config;
import dev.bloom.Theme;
import dev.bloom.module.HudModule;
import dev.bloom.module.Module;
import dev.bloom.module.ModuleManager;
import dev.bloom.ui.Input;
import dev.bloom.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Drag the HUD modules where you want them. */
public class HudEditorScreen extends Screen {
    private final Screen parent;
    private HudModule drag;
    private int offX, offY;

    public HudEditorScreen(Screen parent) {
        super(Component.literal("HUD Editor"));
        this.parent = parent;
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float pt) { }

    @Override
    public void onClose() {
        Config.save();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        Input.poll();
        g.fill(0, 0, width, height, Theme.argb(0x40, 0x000000));
        Ui.centered(g, "HUD Editor: drag the boxes. Esc to go back.", width / 2, 8, Theme.TITLE);

        if (drag != null && !Input.down(0)) {
            drag = null;
            Config.save();
        }
        if (drag != null) drag.dragTo(mx - offX, my - offY, width, height);

        boolean click = Input.pressed(0);
        HudModule hit = null;
        for (Module m : ModuleManager.all()) {
            if (!m.enabled || !(m instanceof HudModule h) || h.lastW <= 0) continue;
            boolean hov = Ui.in(mx, my, h.lastX, h.lastY, h.lastW, h.lastH);
            if (hov) hit = h;
            int col = h == drag ? Theme.ACCENT : Theme.withAlpha(Theme.ACCENT, hov ? 0xFF : 0x90);
            Ui.ring(g, h.lastX - 1, h.lastY - 1, h.lastW + 2, h.lastH + 2, 3, col, 1);
        }
        if (click && hit != null && drag == null) {
            drag = hit;
            offX = mx - hit.lastX;
            offY = my - hit.lastY;
        }
        if (hit != null) Ui.centered(g, hit.name, width / 2, height - 14, Theme.MUTED);
    }
}
