package com.paymentalerts;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;

/** Small 2D drawing toolkit built only on DrawContext.fill / drawText so it is version-robust. */
final class Fx {
    private Fx() {}

    interface ColorFn { int at(int i, int n); }

    // ---------- math ----------
    static float sin(float x) { return (float) Math.sin(x); }
    static float cos(float x) { return (float) Math.cos(x); }
    static float sqrt(float x) { return (float) Math.sqrt(x); }
    static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }
    static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    static float smooth(float t) { t = clamp01(t); return t * t * (3f - 2f * t); }
    static float easeOut(float t) { t = clamp01(t); float u = 1f - t; return 1f - u * u * u; }
    static float easeIn(float t) { t = clamp01(t); return t * t * t; }
    static float env(float t, float d, float in, float out) {
        return Math.min(smooth(t / in), smooth((d - t) / out));
    }

    static float bounce(float x) {
        x = clamp01(x);
        float n1 = 7.5625f, d1 = 2.75f;
        if (x < 1f / d1) return n1 * x * x;
        if (x < 2f / d1) { x -= 1.5f / d1; return n1 * x * x + 0.75f; }
        if (x < 2.5f / d1) { x -= 2.25f / d1; return n1 * x * x + 0.9375f; }
        x -= 2.625f / d1;
        return n1 * x * x + 0.984375f;
    }

    // ---------- deterministic randomness ----------
    static double rnd(long s) {
        s += 0x9E3779B97F4A7C15L;
        s = (s ^ (s >>> 30)) * 0xBF58476D1CE4E5B9L;
        s = (s ^ (s >>> 27)) * 0x94D049BB133111EBL;
        s ^= s >>> 31;
        return (s >>> 11) * (1.0 / (1L << 53));
    }

    static float r(long seed, int i, int k) {
        return (float) rnd(seed * 31L + i * 7919L + k * 104729L + 12345L);
    }

    // ---------- colors ----------
    static int rgb(int r, int g, int b) { return ((r & 255) << 16) | ((g & 255) << 8) | (b & 255); }

    /** rgb + alpha(0..1) -> ARGB */
    static int col(int rgb, float a) {
        int al = (int) (clamp01(a) * 255f + 0.5f);
        return (al << 24) | (rgb & 0xFFFFFF);
    }

    static int mix(int c1, int c2, float t) {
        t = clamp01(t);
        int r1 = (c1 >> 16) & 255, g1 = (c1 >> 8) & 255, b1 = c1 & 255;
        int r2 = (c2 >> 16) & 255, g2 = (c2 >> 8) & 255, b2 = c2 & 255;
        return rgb((int) (r1 + (r2 - r1) * t), (int) (g1 + (g2 - g1) * t), (int) (b1 + (b2 - b1) * t));
    }

    static int hsv(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        float hh = h * 6f;
        int i = (int) hh;
        float f = hh - i;
        float p = v * (1f - s), q = v * (1f - s * f), t = v * (1f - s * (1f - f));
        float r, g, b;
        switch (i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return rgb((int) (r * 255), (int) (g * 255), (int) (b * 255));
    }

    // ---------- shapes ----------
    static void rect(DrawContext c, int x, int y, int w, int h, int color) {
        c.fill(x, y, x + w, y + h, color);
    }

    static void overlay(DrawContext c, int w, int h, int rgb, float alpha) {
        if (alpha <= 0.004f) return;
        c.fill(0, 0, w, h, col(rgb, alpha));
    }

    static void dot(DrawContext c, float x, float y, int size, int color) {
        int ix = Math.round(x - size / 2f), iy = Math.round(y - size / 2f);
        c.fill(ix, iy, ix + size, iy + size, color);
    }

    static void ellipse(DrawContext c, float cx, float cy, float rx, float ry, int color) {
        if (rx < 0.5f || ry < 0.5f) return;
        int step = Math.max(1, (int) (ry / 36f));
        for (float dy = -ry; dy < ry; dy += step) {
            float k = dy / ry;
            float half = rx * sqrt(Math.max(0f, 1f - k * k));
            int y0 = Math.round(cy + dy);
            int x0 = Math.round(cx - half), x1 = Math.round(cx + half);
            if (x1 > x0) c.fill(x0, y0, x1, y0 + step, color);
        }
    }

    static void disc(DrawContext c, float cx, float cy, float r, int color) {
        ellipse(c, cx, cy, r, r, color);
    }

    /** Soft glow: stacked translucent discs. */
    static void glow(DrawContext c, float cx, float cy, float r, int rgb, float alpha, int layers) {
        if (alpha <= 0.004f) return;
        for (int i = layers; i >= 1; i--) {
            float k = i / (float) layers;
            disc(c, cx, cy, r * k, col(rgb, alpha / layers * 1.7f));
        }
    }

    static void ring(DrawContext c, float cx, float cy, float rx, float ry, int thick, int color) {
        int n = Math.min(360, Math.max(16, (int) (Math.max(rx, ry) * 0.9f)));
        for (int i = 0; i < n; i++) {
            float a = (float) (i * 2.0 * Math.PI / n);
            dot(c, cx + cos(a) * rx, cy + sin(a) * ry, thick, color);
        }
    }

    static void line(DrawContext c, float x1, float y1, float x2, float y2, int thick, int color) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = sqrt(dx * dx + dy * dy);
        int n = Math.max(1, (int) (len / Math.max(1f, thick * 0.8f)));
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            dot(c, x1 + dx * t, y1 + dy * t, thick, color);
        }
    }

    static void panel(DrawContext c, int x, int y, int w, int h, int bg, int border) {
        c.fill(x, y, x + w, y + h, border);
        c.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);
    }

    static void vignette(DrawContext c, int w, int h, int rgb, float alpha, int th) {
        if (alpha <= 0.004f) return;
        int n = 14;
        for (int i = 0; i < n; i++) {
            float k = 1f - i / (float) n;
            int a = col(rgb, alpha * k * k);
            int s0 = th * i / n, s1 = th * (i + 1) / n;
            if (s1 <= s0) s1 = s0 + 1;
            c.fill(0, s0, w, s1, a);
            c.fill(0, h - s1, w, h - s0, a);
            c.fill(s0, 0, s1, h, a);
            c.fill(w - s1, 0, w - s0, h, a);
        }
    }

    // ---------- text ----------
    static TextRenderer tr() { return MinecraftClient.getInstance().textRenderer; }

    static int textWidth(String s) { return tr().getWidth(s); }

    /** Centered text at (cx, y) with a scale. "bold" fakes bold by double drawing. */
    static void text(DrawContext c, String s, float cx, float y, float scale, int color, boolean bold) {
        if (((color >>> 24) & 255) < 6) return;
        TextRenderer t = tr();
        var m = c.getMatrices();
        m.pushMatrix();
        m.translate(cx, y);
        m.scale(scale, scale);
        int x = -t.getWidth(s) / 2;
        if (bold) c.drawText(t, s, x + 1, 0, color, false);
        c.drawText(t, s, x, 0, color, true);
        m.popMatrix();
    }

    /** Centered text where each character gets its own color. */
    static void textFn(DrawContext c, String s, float cx, float y, float scale, ColorFn fn, float alpha, boolean bold) {
        if (alpha < 0.03f) return;
        TextRenderer t = tr();
        var m = c.getMatrices();
        m.pushMatrix();
        m.translate(cx, y);
        m.scale(scale, scale);
        int total = t.getWidth(s);
        int x = -total / 2;
        int n = s.length();
        for (int i = 0; i < n; i++) {
            String ch = String.valueOf(s.charAt(i));
            int color = col(fn.at(i, n), alpha);
            if (bold) c.drawText(t, ch, x + 1, 0, color, false);
            c.drawText(t, ch, x, 0, color, true);
            x += t.getWidth(ch);
        }
        m.popMatrix();
    }

    static ColorFn grad(int a, int b) {
        return (i, n) -> mix(a, b, n <= 1 ? 0f : i / (float) (n - 1));
    }

    static ColorFn rainbow(float shift) {
        return (i, n) -> hsv(i / (float) Math.max(1, n) * 0.85f + shift, 0.55f, 1f);
    }

    static ColorFn solid(int rgb) { return (i, n) -> rgb; }

    /** Name + sub-line on a dark bordered card, centered at top (y). Returns card height. */
    static int card(DrawContext c, int w, int y, String name, String sub, float al, ColorFn fn, int borderRgb, float ns) {
        if (al < 0.02f) return 0;
        int nw = (int) (textWidth(name) * ns);
        int sw = textWidth(sub);
        int pw = Math.max(Math.max(nw, sw) + 30, 110);
        int nameH = (int) (9 * ns);
        int ph = nameH + 9 + 14;
        int x = (w - pw) / 2;
        panel(c, x, y, pw, ph, col(0x101626, 0.82f * al), col(borderRgb, al));
        textFn(c, name, w / 2f, y + 6, ns, fn, al, true);
        text(c, sub, w / 2f, y + 6 + nameH + 4, 1f, col(0xE8ECF4, al), false);
        return ph;
    }

    // ---------- player head ----------
    static void head(DrawContext c, String name, int x, int y, int size) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayNetworkHandler net = mc.getNetworkHandler();
        PlayerListEntry e = null;
        if (net != null) {
            try { e = net.getPlayerListEntry(name); } catch (Exception ignored) { }
        }
        if (e != null) {
            PlayerSkinDrawer.draw(c, e.getSkinTextures(), x, y, size);
            return;
        }
        // Fallback: a little pixel face when the player is not in the tab list.
        float u = size / 8f;
        c.fill(x, y, x + size, y + size, 0xFFB98A5E);
        px(c, x, y, u, 0, 0, 8, 2, 0xFF3B2A1A);
        px(c, x, y, u, 0, 2, 1, 1, 0xFF3B2A1A);
        px(c, x, y, u, 7, 2, 1, 1, 0xFF3B2A1A);
        px(c, x, y, u, 1, 4, 1, 1, 0xFFFFFFFF);
        px(c, x, y, u, 2, 4, 1, 1, 0xFF3A3AA0);
        px(c, x, y, u, 5, 4, 1, 1, 0xFF3A3AA0);
        px(c, x, y, u, 6, 4, 1, 1, 0xFFFFFFFF);
        px(c, x, y, u, 3, 5, 2, 1, 0xFF8B5E3C);
        px(c, x, y, u, 2, 6, 4, 1, 0xFF5E3B26);
    }

    private static void px(DrawContext c, int x, int y, float u, int col, int row, int w, int h, int color) {
        c.fill(x + Math.round(col * u), y + Math.round(row * u),
                x + Math.round((col + w) * u), y + Math.round((row + h) * u), color);
    }
}
