package dev.bloom.visual;

import dev.bloom.Theme;
import dev.bloom.ui.Ui;
import net.minecraft.client.gui.GuiGraphics;

/** A blocky character for the main menu. A skin-based painter can replace the default one. */
public final class Avatar {
    public interface Painter {
        /**
         * @param x      centre of the character
         * @param y      ground line (feet)
         * @param u      size of one skin pixel in GUI pixels
         * @param walk   walk-cycle phase in radians
         * @param moving true while walking
         * @param lookX  -1..1, where the head is looking horizontally
         * @param lookY  -1..1, where the head is looking vertically
         */
        void paint(GuiGraphics g, int x, int y, int u, double walk, boolean moving, double lookX, double lookY, double time);
    }

    public static Painter painter = Avatar::paintBlocky;

    private Avatar() { }

    public static void paint(GuiGraphics g, int x, int y, int u, double walk, boolean moving, double lookX, double lookY, double time) {
        try {
            painter.paint(g, x, y, u, walk, moving, lookX, lookY, time);
        } catch (Throwable t) {
            painter = Avatar::paintBlocky;
            paintBlocky(g, x, y, u, walk, moving, lookX, lookY, time);
        }
    }

    /** Joint offsets shared by all painters, in skin pixels. */
    public static final class Pose {
        public double bob, legL, legR, armL, armR, headX, headY, bodyX;

        public static Pose of(double walk, boolean moving, double lookX, double lookY, double time) {
            Pose p = new Pose();
            if (moving) {
                double s = Math.sin(walk);
                p.bob = Math.abs(s) * 0.7;
                p.legL = Math.max(0, s) * 2.5;
                p.legR = Math.max(0, -s) * 2.5;
                p.armL = -s * 1.8;
                p.armR = s * 1.8;
            } else {
                p.bob = Math.sin(time * 2.0) * 0.3;
                p.armL = Math.sin(time * 2.0) * 0.4;
                p.armR = -Math.sin(time * 2.0) * 0.4;
            }
            p.headX = lookX * 1.3;
            p.headY = lookY * 0.8;
            p.bodyX = lookX * 0.35;
            return p;
        }
    }

    private static int shade(int rgb, double k) {
        int r = (int) Math.max(0, Math.min(255, ((rgb >> 16) & 255) * k));
        int g = (int) Math.max(0, Math.min(255, ((rgb >> 8) & 255) * k));
        int b = (int) Math.max(0, Math.min(255, (rgb & 255) * k));
        return (r << 16) | (g << 8) | b;
    }

    private static void part(GuiGraphics g, int x0, int y0, int w, int h, int rgb, int a, int u) {
        if (w <= 0 || h <= 0) return;
        int col = Theme.argb(a, rgb);
        g.fill(x0, y0, x0 + w, y0 + h, col);
        int sh = Math.max(1, h / 5);
        g.fill(x0, y0 + h - sh, x0 + w, y0 + h, Theme.argb(a, shade(rgb, 0.78)));
        if (Theme.glass) Ui.ring(g, x0, y0, w, h, 0, Theme.argb(0xE0, 0xFFFFFF), 1);
    }

    public static void paintBlocky(GuiGraphics g, int x, int y, int u, double walk, boolean moving,
                                   double lookX, double lookY, double time) {
        Pose p = Pose.of(walk, moving, lookX, lookY, time);
        int a = Theme.glass ? 0xA0 : 0xFF;
        int skin = 0xD9A87E, hair = 0x4A2F1C, shirt = Theme.glass ? 0xCFEFFF : Theme.accent, pants = 0x35407A, shoe = 0x555566;

        int bob = (int) Math.round(p.bob * u);
        int bx = x + (int) Math.round(p.bodyX * u);
        int top = y - 32 * u - bob;                                   // top of head
        // legs (a lifted leg gets shorter)
        int legTop = y - 12 * u - bob;
        int lh = 12 * u;
        part(g, bx - 4 * u, legTop, 4 * u, lh - (int) Math.round(p.legL * u) + bob, pants, a, u);
        part(g, bx, legTop, 4 * u, lh - (int) Math.round(p.legR * u) + bob, pants, a, u);
        part(g, bx - 4 * u, y - 2 * u, 4 * u, 2 * u - (int) Math.round(p.legL * u) / 2 + 0, shoe, a, u);
        part(g, bx, y - 2 * u, 4 * u, 2 * u - (int) Math.round(p.legR * u) / 2 + 0, shoe, a, u);
        // body
        int bodyTop = y - 24 * u - bob;
        part(g, bx - 4 * u, bodyTop, 8 * u, 12 * u, shirt, a, u);
        // arms
        part(g, bx - 8 * u, bodyTop + (int) Math.round(p.armL * u), 4 * u, 12 * u, shirt, a, u);
        part(g, bx + 4 * u, bodyTop + (int) Math.round(p.armR * u), 4 * u, 12 * u, shirt, a, u);
        part(g, bx - 8 * u, bodyTop + 9 * u + (int) Math.round(p.armL * u), 4 * u, 3 * u, skin, a, u);
        part(g, bx + 4 * u, bodyTop + 9 * u + (int) Math.round(p.armR * u), 4 * u, 3 * u, skin, a, u);
        // head
        int hx = bx - 4 * u + (int) Math.round(p.headX * u), hy = top + (int) Math.round(p.headY * u);
        part(g, hx, hy, 8 * u, 8 * u, skin, a, u);
        part(g, hx, hy, 8 * u, 2 * u, hair, a, u);
        part(g, hx, hy, u, 4 * u, hair, a, u);
        part(g, hx + 7 * u, hy, u, 4 * u, hair, a, u);
        // eyes follow the mouse
        int ex = (int) Math.round(lookX * u * 0.9), ey = (int) Math.round(lookY * u * 0.5);
        for (int side = 0; side < 2; side++) {
            int ox = hx + (side == 0 ? 1 : 5) * u, oy = hy + 4 * u;
            g.fill(ox, oy, ox + 2 * u, oy + 2 * u, Theme.argb(a, 0xFFFFFF));
            g.fill(ox + (u / 2) + ex / 2 + (side == 0 ? 0 : 0), oy + (u / 2) + ey, ox + (u / 2) + ex / 2 + u, oy + (u / 2) + ey + u, Theme.argb(a, 0x2A55C8));
        }
        g.fill(hx + 3 * u, hy + 6 * u, hx + 5 * u, hy + 6 * u + Math.max(1, u / 2), Theme.argb(a, 0x7A3B2E));
    }
}
