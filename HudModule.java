package dev.bloom.module;

import dev.bloom.Config;
import dev.bloom.Theme;
import dev.bloom.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** A module drawn on screen. Position is stored as a fraction of the free screen space. */
public abstract class HudModule extends Module {
    public double fx, fy;
    public final Setting.Num scale;
    public final Setting.Bool background;
    public final Setting.Color color;
    /** Last drawn box in GUI pixels (used by the HUD editor). */
    public int lastX, lastY, lastW, lastH;

    protected HudModule(String id, String name, String category, String description,
                        boolean defaultOn, double defX, double defY) {
        super(id, name, category, description, defaultOn);
        this.fx = Config.getDouble("hud." + id + ".x", defX);
        this.fy = Config.getDouble("hud." + id + ".y", defY);
        this.scale = add(new Setting.Num("scale", "Scale", 0.6, 2.0, 0.1, 1.0));
        this.background = add(new Setting.Bool("background", "Background", true));
        this.color = add(new Setting.Color("color", "Text Color", 0xFFFFFF));
    }

    public void setPos(double x, double y) {
        fx = Math.max(0, Math.min(1, x));
        fy = Math.max(0, Math.min(1, y));
        Config.put("hud." + id + ".x", Double.toString(fx));
        Config.put("hud." + id + ".y", Double.toString(fy));
    }

    /** Size of the content in unscaled GUI pixels: {width, height}. */
    protected abstract int[] measure(Minecraft mc);

    /** Draw the content with its top-left corner at (0, 0). */
    protected abstract void draw(GuiGraphics g, Minecraft mc, int w, int h);

    public void renderAt(GuiGraphics g, Minecraft mc, int gw, int gh) {
        int[] sz = measure(mc);
        int w = sz[0], h = sz[1];
        if (w <= 0 || h <= 0) return;
        float sc = (float) scale.value;
        int pw = Math.round(w * sc), ph = Math.round(h * sc);
        int x = (int) Math.round(fx * Math.max(0, gw - pw));
        int y = (int) Math.round(fy * Math.max(0, gh - ph));
        lastX = x;
        lastY = y;
        lastW = pw;
        lastH = ph;
        var m = g.pose();
        m.pushMatrix();
        m.translate((float) x, (float) y);
        m.scale(sc, sc);
        try {
            if (background.value) Ui.hudBg(g, 0, 0, w, h);
            draw(g, mc, w, h);
        } finally {
            m.popMatrix();
        }
    }

    /** Move so the box's top-left corner is at (x, y) in GUI pixels. */
    public void dragTo(int x, int y, int gw, int gh) {
        int free = gw - lastW, freeY = gh - lastH;
        setPos(free > 0 ? x / (double) free : 0, freeY > 0 ? y / (double) freeY : 0);
    }

    protected int textColor() {
        return Theme.glass && color.rgb == 0xFFFFFF ? 0xFFFFFFFF : color.argb();
    }
}
