package dev.bloom.lyrics;

import dev.bloom.Config;
import dev.bloom.Theme;
import dev.bloom.module.HudModule;
import dev.bloom.module.Setting;
import dev.bloom.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Lyrics in the Air: the current lyric floats on screen, bobbing letter by letter. */
public final class LyricsModule extends HudModule {
    private final Setting.Num wave = add(new Setting.Num("wave", "Wave", 0, 6, 0.5, 2.5));
    private final Setting.Bool showNext = add(new Setting.Bool("next", "Show Next Line", true));
    private final Setting.Bool glow = add(new Setting.Bool("glow", "Glow", true));

    public LyricsModule() {
        super("lyrics", "Lyrics in the Air", "Music", "Floating lyrics for a song you add (Scale = size).", false, 0.5, 0.26);
        if (Config.get("mod.lyrics.background", null) == null) background.value = false;
        String saved = Config.get("lyrics.song", null);
        if (saved != null && LyricsEngine.list().contains(saved)) LyricsEngine.select(saved);
    }

    @Override
    public void dragTo(int x, int y, int gw, int gh) {
        setPos((x + lastW / 2.0) / gw, (y - 0.0) / gh);
    }

    @Override protected int[] measure(Minecraft mc) { return new int[] { 0, 0 }; }
    @Override protected void draw(GuiGraphics g, Minecraft mc, int w, int h) { }

    private static int alpha(int a, int rgbArgb) {
        return Theme.withAlpha(rgbArgb, Math.max(0, Math.min(255, a)));
    }

    @Override
    public void renderAt(GuiGraphics g, Minecraft mc, int gw, int gh) {
        LyricsEngine.Frame f = LyricsEngine.frame();
        int cx = (int) Math.round(fx * gw), cy = (int) Math.round(fy * gh);
        float size = (float) scale.value * 1.5f;
        if (f == null) {
            Ui.centered(g, "Lyrics in the Air: pick a song in the Bloom menu (Lyrics tab)", cx, cy, alpha(170, textColor()));
            lastX = cx - 100; lastY = cy; lastW = 200; lastH = 10;
            return;
        }
        String cur = f.cur.isEmpty() ? (f.playing ? "..." : "Press play in the Bloom menu") : f.cur;
        double t = System.nanoTime() / 1.0e9;
        float fade = Math.min(1f, f.sinceMs / 350f);
        fade = 1f - (1f - fade) * (1f - fade);
        int a = f.cur.isEmpty() ? 200 : (int) (255 * fade);
        float lift = (1f - fade) * 6f;

        int textW = Ui.width(cur);
        int boxW = (int) (textW * size) + 16, boxH = (int) (10 * size) + 10;
        lastX = cx - boxW / 2; lastY = cy - 4; lastW = boxW; lastH = boxH;

        var m = g.pose();
        m.pushMatrix();
        m.translate((float) cx, (float) cy + lift);
        m.scale(size, size);
        try {
            if (background.value) Ui.hudBg(g, -textW / 2 - 6, -4, textW + 12, 17);
            double amp = wave.value;
            int x = -textW / 2;
            int base = Theme.glass ? 0xFFFFFFFF : textColor();
            for (int i = 0; i < cur.length(); i++) {
                String ch = String.valueOf(cur.charAt(i));
                int dy = (int) Math.round(Math.sin(t * 2.4 + i * 0.45) * amp * 0.5);
                if (glow.value) {
                    int gc = alpha(a / 4, 0xFF000000 | Theme.accent);
                    Ui.textFlat(g, ch, x - 1, dy, gc);
                    Ui.textFlat(g, ch, x + 1, dy, gc);
                    Ui.textFlat(g, ch, x, dy - 1, gc);
                    Ui.textFlat(g, ch, x, dy + 1, gc);
                }
                if (a > 8) Ui.text(g, ch, x, dy, alpha(a, base));
                x += Ui.width(ch);
            }
        } finally {
            m.popMatrix();
        }
        if (showNext.value && !f.next.isEmpty()) {
            float ns = Math.max(0.8f, size * 0.55f);
            var m2 = g.pose();
            m2.pushMatrix();
            m2.translate((float) cx, (float) cy + 10 * size + 6);
            m2.scale(ns, ns);
            try {
                Ui.centered(g, f.next, 0, 0, alpha(150, textColor()));
            } finally {
                m2.popMatrix();
            }
        }
    }
}
