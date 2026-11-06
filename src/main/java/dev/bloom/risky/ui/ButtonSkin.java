package dev.bloom.risky.ui;

import dev.bloom.Config;
import dev.bloom.ui.Input;
import dev.bloom.ui.Ui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;

/** Draws vanilla buttons in the Bloom theme (colors, Glass, roundness all follow the Theme tab). */
public final class ButtonSkin {
    private static boolean failed;

    private ButtonSkin() { }

    /** @return true if the button was drawn here and vanilla drawing should be skipped */
    public static boolean draw(AbstractWidget w, GuiGraphics g) {
        if (failed || !Config.getBool("ui.skin", true)) return false;
        int bw = w.getWidth(), bh = w.getHeight();
        if (bw < 12 || bh < 10) return false;
        Ui.styledButton(g, w.getX(), w.getY(), bw, bh, w.getMessage().getString(), w.isHoveredOrFocused(), w.active);
        return true;
    }

    public static void fail(Throwable t) {
        failed = true;
        System.out.println("[Bloom] button styling turned itself off after an error: " + t);
    }
}
