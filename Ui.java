package dev.bloom.ui;

import dev.bloom.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Rounded-shape drawing with supersampled corners. A shape is cut into rects on the GUI pixel
 * grid, so Minecraft layers overlapping shapes in the order they were drawn (no stray lines).
 */
public final class Ui {
    private static final HashMap<Long, int[]> CACHE = new HashMap<>();

    private Ui() { }

    // ------------------------------------------------------------------ text

    public static Font font() { return Minecraft.getInstance().font; }
    public static int width(String s) { return font().width(s); }

    public static void text(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(font(), s, x, y, color);
    }

    public static void textFlat(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(font(), s, x, y, color, false);
    }

    public static void centered(GuiGraphics g, String s, int cx, int y, int color) {
        g.drawString(font(), s, cx - width(s) / 2, y, color);
    }

    public static String fit(String s, int maxW) {
        if (width(s) <= maxW) return s;
        return font().plainSubstrByWidth(s, Math.max(0, maxW - width("..."))) + "...";
    }

    public static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    // ------------------------------------------------------------------ shapes

    public static void rrect(GuiGraphics g, int x, int y, int w, int h, int r, int fill) {
        draw(g, x, y, w, h, r, fill, 0, 0);
    }

    public static void rrect(GuiGraphics g, int x, int y, int w, int h, int r, int fill, int border, int borderPx) {
        draw(g, x, y, w, h, r, fill, border, borderPx);
    }

    /** Hollow outline: a ring of {@code px} GUI pixels around the box, nothing inside. */
    public static void ring(GuiGraphics g, int x, int y, int w, int h, int r, int color, int px) {
        draw(g, x, y, w, h, r, 0, color, px);
    }

    private static int samples() {
        double s = Minecraft.getInstance().getWindow().getGuiScale();
        return (int) Math.max(1, Math.min(4, Math.floor(s + 0.01)));
    }

    private static void draw(GuiGraphics g, int x, int y, int w, int h, int r, int fill, int border, int bw) {
        if (w <= 0 || h <= 0) return;
        int s = samples();
        int[] p = pieces(w * s, h * s, Math.max(0, Math.min(r, Math.min(w, h) / 2)) * s, bw * s, s);
        var m = g.pose();
        if (s > 1) {
            m.pushMatrix();
            m.scale(1f / s, 1f / s);
        }
        try {
            int bx = x * s, by = y * s;
            for (int i = 0; i < p.length; i += 5) {
                int col = p[i + 4] == 0 ? fill : border;
                if ((col >>> 24) == 0) continue;
                g.fill(bx + p[i], by + p[i + 1], bx + p[i + 2], by + p[i + 3], col);
            }
        } finally {
            if (s > 1) m.popMatrix();
        }
    }

    private static int[] pieces(int w, int h, int r, int b, int s) {
        long key = ((long) w << 44) ^ ((long) h << 28) ^ ((long) r << 12) ^ ((long) b << 4) ^ s;
        int[] p = CACHE.get(key);
        if (p == null) {
            if (CACHE.size() > 900) CACHE.clear();
            p = build(w, h, r, b, s);
            CACHE.put(key, p);
        }
        return p;
    }

    private static float inset(float d, float r) {
        if (d >= r) return 0f;
        float t = r - d;
        float v = r * r - t * t;
        if (v < 0f) v = 0f;
        return r - (float) Math.sqrt(v);
    }

    /** Segments of one sample row: triples (x0, x1, kind) where kind 0 = fill, 1 = border. */
    private static int[] rowSegs(int y, int w, int h, float r, float rin, int b) {
        float outer = 0f, inner = 0f;
        boolean innerRow = b > 0 && y >= b && y < h - b;
        for (int j = 0; j < 3; j++) {
            float yy = y + (j + 0.5f) / 3f;
            float d = Math.min(yy, h - yy);
            outer += inset(d, r);
            if (innerRow) inner += b + inset(d - b, rin);
        }
        outer /= 3f;
        inner /= 3f;
        int xo = Math.round(outer);
        int[] segs = new int[9];
        int n = 0;
        if (innerRow && inner < w / 2f - 1f) {
            int xi = Math.round(inner);
            if (xi > xo) {
                segs[n++] = xo; segs[n++] = xi; segs[n++] = 1;
                segs[n++] = w - xi; segs[n++] = w - xo; segs[n++] = 1;
            }
            if (w - xi > xi) { segs[n++] = xi; segs[n++] = w - xi; segs[n++] = 0; }
        } else if (w - xo > xo) {
            segs[n++] = xo; segs[n++] = w - xo; segs[n++] = b > 0 ? 1 : 0;
        }
        return Arrays.copyOf(segs, n);
    }

    static int[] build(int w, int h, int rSamples, int b, int s) {
        float r = Math.min(rSamples, Math.min(w, h) / 2f);
        float rin = Math.max(0f, r - b);
        List<int[]> out = new ArrayList<>();
        int[] prev = null;
        int start = 0;
        for (int y = 0; y <= h; y++) {
            int[] cur = y < h ? rowSegs(y, w, h, r, rin, b) : null;
            if (prev != null && cur != null && Arrays.equals(prev, cur)) continue;
            if (prev != null) {
                for (int i = 0; i < prev.length; i += 3) addSplit(out, prev[i], start, prev[i + 1], y, prev[i + 2], s);
            }
            prev = cur;
            start = y;
        }
        int[] flat = new int[out.size() * 5];
        for (int i = 0; i < out.size(); i++) System.arraycopy(out.get(i), 0, flat, i * 5, 5);
        return flat;
    }

    private static int[] cuts(int a, int b, int s) {
        int h = Math.floorDiv(a + s - 1, s) * s;
        int t = Math.floorDiv(b, s) * s;
        int[] o = new int[4];
        int n = 0;
        o[n++] = a;
        if (h > a && h < b) o[n++] = h;
        if (t > o[n - 1] && t < b) o[n++] = t;
        o[n++] = b;
        return Arrays.copyOf(o, n);
    }

    private static void addSplit(List<int[]> out, int x0, int y0, int x1, int y1, int kind, int s) {
        if (x1 <= x0 || y1 <= y0) return;
        if (s <= 1) {
            out.add(new int[] { x0, y0, x1, y1, kind });
            return;
        }
        int[] cx = cuts(x0, x1, s), cy = cuts(y0, y1, s);
        for (int j = 0; j < cy.length - 1; j++)
            for (int i = 0; i < cx.length - 1; i++)
                out.add(new int[] { cx[i], cy[j], cx[i + 1], cy[j + 1], kind });
    }

    // ------------------------------------------------------------------ themed pieces

    /** Big panel: normal = tinted translucent with an accent border, glass = clear with a bright rim and gloss. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, int r) {
        rrect(g, x, y, w, h, r, Theme.PANEL, Theme.BORDER, 1);
        if (Theme.glass) gloss(g, x + 2, y + 2, w - 4, Math.max(4, h / 3), Math.max(2, r - 2));
    }

    public static void gloss(GuiGraphics g, int x, int y, int w, int h, int r) {
        rrect(g, x, y, w, h, r, Theme.argb(0x1E, 0xFFFFFF));
    }

    public static void card(GuiGraphics g, int x, int y, int w, int h, int r, boolean hover) {
        rrect(g, x, y, w, h, r, hover ? Theme.CARD_HOVER : Theme.CARD);
        if (Theme.glass) ring(g, x, y, w, h, r, Theme.argb(hover ? 0x90 : 0x55, 0xFFFFFF), 1);
    }

    public static void button(GuiGraphics g, int x, int y, int w, int h, String label, boolean hover) {
        rrect(g, x, y, w, h, Math.max(2, Theme.radius - 1), hover ? Theme.BTN_HOVER : Theme.BTN);
        if (Theme.glass) ring(g, x, y, w, h, Math.max(2, Theme.radius - 1), Theme.argb(hover ? 0xC0 : 0x80, 0xFFFFFF), 1);
        centered(g, label, x + w / 2, y + (h - 8) / 2, Theme.TEXT);
    }

    public static void toggle(GuiGraphics g, int x, int y, boolean on) {
        rrect(g, x, y, 22, 11, 5, on ? Theme.ACCENT : Theme.argb(0x90, 0x202030));
        if (Theme.glass) ring(g, x, y, 22, 11, 5, Theme.argb(0xA0, 0xFFFFFF), 1);
        int kx = on ? x + 12 : x + 1;
        rrect(g, kx, y + 1, 9, 9, 4, on && Theme.glass ? 0xFFFFFFFF : 0xFFF2F2F8);
    }

    public static void slider(GuiGraphics g, int x, int y, int w, double frac) {
        rrect(g, x, y + 3, w, 4, 2, Theme.argb(0x90, 0x202030));
        int fw = (int) Math.round(Math.max(0, Math.min(1, frac)) * w);
        if (fw > 0) rrect(g, x, y + 3, Math.max(2, fw), 4, 2, Theme.ACCENT);
        rrect(g, x + Math.max(0, Math.min(w - 6, fw - 3)), y + 1, 6, 8, 3, 0xFFFFFFFF);
    }

    /** Background behind a HUD module. */
    public static void hudBg(GuiGraphics g, int x, int y, int w, int h) {
        int r = Math.max(2, Math.min(Theme.radius, 5));
        if (Theme.glass) {
            rrect(g, x, y, w, h, r, Theme.argb(0x34, 0xDDF2FF));
            ring(g, x, y, w, h, r, Theme.argb(0xB0, 0xFFFFFF), 1);
        } else {
            rrect(g, x, y, w, h, r, Theme.argb(0x9A, 0x000000 | (Theme.PANEL & 0xFFFFFF)), Theme.argb(0xC0, Theme.accent), 1);
        }
    }

    /** Slow rising bubbles inside a box (glass theme decoration). */
    public static void bubbles(GuiGraphics g, int x, int y, int w, int h) {
        if (w < 40 || h < 40) return;
        double t = System.nanoTime() / 1.0e9;
        for (int i = 0; i < 14; i++) {
            long seed = i * 2654435761L;
            double fx = ((seed >> 3) & 1023) / 1023.0;
            int size = 5 + (int) ((seed >> 13) & 7) * 2;
            double speed = 6 + ((seed >> 7) & 15);
            double cycle = h + size * 2;
            double yy = cycle - ((t * speed + ((seed >> 17) & 255)) % cycle);
            int bx = x + 4 + (int) (fx * (w - size - 8)) + (int) (Math.sin(t * 0.8 + i) * 3);
            int by = y + (int) yy - size;
            if (by < y + 1 || by + size > y + h - 1) continue;
            rrect(g, bx, by, size, size, size / 2, Theme.argb(0x16, 0xFFFFFF), Theme.argb(0x70, 0xFFFFFF), 1);
            rrect(g, bx + 1, by + 1, Math.max(2, size / 3), Math.max(2, size / 3), 1, Theme.argb(0x90, 0xFFFFFF));
        }
    }
}
