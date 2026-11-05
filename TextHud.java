package dev.bloom.module;

import dev.bloom.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/** A HUD module that is just a few lines of text. */
public abstract class TextHud extends HudModule {
    protected TextHud(String id, String name, String description, boolean defaultOn, double x, double y) {
        super(id, name, "HUD", description, defaultOn, x, y);
    }

    protected abstract List<String> lines(Minecraft mc);

    @Override
    protected int[] measure(Minecraft mc) {
        List<String> ls = lines(mc);
        if (ls.isEmpty()) return new int[] { 0, 0 };
        int w = 0;
        for (String s : ls) w = Math.max(w, Ui.width(s));
        return new int[] { w + 8, ls.size() * 10 + 4 };
    }

    @Override
    protected void draw(GuiGraphics g, Minecraft mc, int w, int h) {
        int y = 3;
        for (String s : lines(mc)) {
            Ui.text(g, s, 4, y, textColor());
            y += 10;
        }
    }
}
