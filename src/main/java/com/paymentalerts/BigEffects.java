package com.paymentalerts;

import net.minecraft.client.gui.DrawContext;

/** Jackpot, nuke, orbital laser, black hole and god tier. */
final class BigEffects {
    private BigEffects() {}

    // =====================================================================
    // $500M JACKPOT (slot machine)
    // =====================================================================
    private static final int SYM_DOLLAR = 0, SYM_SEVEN = 1, SYM_STAR = 2, SYM_DIAMOND = 3, SYM_BAR = 4, SYM_CHERRY = 5;

    static void jackpot(DrawContext c, Alert a, int w, int h, float t, float d) {
        final float tWin = 3.8f;
        int win = a.variation % 6;
        float drop = -150f * (1f - Fx.bounce(t / 0.65f)) - 170f * Fx.easeIn((t - (d - 0.8f)) / 0.8f);
        int mx = w / 2 - 96;
        int my = Math.round(8 + drop);
        float cxm = w / 2f;
        boolean won = t >= tWin;

        if (won) {
            float pulse = 0.5f + 0.5f * Fx.sin(t * 8f);
            Fx.glow(c, cxm, my + 52, 150f, 0xFFD34D, 0.16f + 0.10f * pulse, 6);
        }

        // frame
        Fx.rect(c, mx, my, 192, 104, 0xFFD9A441);
        Fx.rect(c, mx + 2, my + 2, 188, 100, 0xFF8C5A10);
        Fx.rect(c, mx + 3, my + 3, 186, 98, 0xFFB3122A);
        // marquee
        Fx.rect(c, mx + 8, my + 7, 176, 24, 0xFF2A0714);
        for (int i = 0; i < 21; i++) {
            boolean on = ((i + (int) (t * (won ? 12f : 6f))) % 2) == 0;
            int col = on ? 0xFFFFE066 : 0xFF7A5A10;
            Fx.rect(c, mx + 12 + i * 8, my + 9, 2, 2, col);
            Fx.rect(c, mx + 12 + i * 8, my + 28, 2, 2, col);
        }
        final float tt = t;
        Fx.ColorFn marquee = won
                ? (i, n) -> (((int) (tt * 8f) + i) % 2 == 0) ? 0xFFD34D : 0xFFFFFF
                : (i, n) -> Fx.hsv(i / (float) n * 0.8f + tt * 0.7f, 0.55f, 1f);
        Fx.textFn(c, "JACKPOT", cxm, my + 12, 1.9f, marquee, 1f, true);

        // reels
        Fx.rect(c, mx + 10, my + 36, 172, 58, 0xFF3A0610);
        float[] stops = {2.3f, 3.0f, 3.7f};
        for (int reel = 0; reel < 3; reel++) {
            int[] strip = {0, 1, 2, 3, 4, 5};
            for (int i = 5; i > 0; i--) {
                int j = (int) (Fx.r(a.seed, reel * 10 + i, 30) * (i + 1));
                int tmp = strip[i]; strip[i] = strip[j]; strip[j] = tmp;
            }
            int widx = 0;
            for (int i = 0; i < 6; i++) if (strip[i] == win) widx = i;
            float p = reelPos(t, widx, 0.5f, stops[reel] - 0.9f, stops[reel]);

            int rx = mx + 14 + reel * 56;
            int ry = my + 38;
            int rh = 54;
            Fx.rect(c, rx, ry, 52, rh, 0xFFF4ECD2);
            c.enableScissor(rx, ry, rx + 52, ry + rh);
            float cy = ry + rh / 2f;
            int k0 = (int) Math.floor(p) - 1;
            for (int k = k0; k <= k0 + 3; k++) {
                float yy = cy + (p - k) * 36f;
                symbol(c, strip[((k % 6) + 6) % 6], rx + 26, yy);
            }
            c.disableScissor();
            c.fillGradient(rx, ry, rx + 52, ry + 14, 0x66000000, 0x00000000);
            c.fillGradient(rx, ry + rh - 14, rx + 52, ry + rh, 0x00000000, 0x66000000);
        }

        // winner text
        if (won) {
            float al = Fx.smooth((t - tWin) / 0.5f) * Fx.smooth((d - t) / 0.8f);
            Fx.textFn(c, a.player, cxm, my + 112, 1.8f, Fx.grad(0xFFE08A, 0xFFB300), al, true);
            Fx.text(c, "paid you " + a.amountText, cxm, my + 112 + 20, 1f, Fx.col(0xFFFFFF, al), false);
            Effects.rain(c, a.seed, w, h, t, tWin, d - tWin - 3.5f, 4, 70, Fx.smooth((d - t) / 1.2f));
            Effects.fireworks(c, a.seed, w, h, t, tWin, 0.7f, 6, 1);
        }
    }

    private static float reelPos(float t, int widx, float tStart, float tSlow, float tStop) {
        if (t < tStart) return 0f;
        float speed = 15f;
        float p0 = speed * (tSlow - tStart);
        if (t < tSlow) return speed * (t - tStart);
        int base = (int) Math.ceil(p0 + 1.5f);
        int target = base + (((widx - base) % 6) + 6) % 6;
        if (t >= tStop) return target;
        float u = (t - tSlow) / (tStop - tSlow);
        return Fx.lerp(p0, target, Fx.easeOut(u));
    }

    private static void symbol(DrawContext c, int kind, float cx, float cy) {
        int ix = Math.round(cx), iy = Math.round(cy);
        switch (kind) {
            case SYM_DOLLAR:
                Fx.text(c, "$", cx, cy - 14f, 3.2f, 0xFF1FA34A, true);
                break;
            case SYM_SEVEN:
                Fx.text(c, "7", cx, cy - 14f, 3.2f, 0xFFE0202F, true);
                break;
            case SYM_STAR: {
                int s = 3;
                int[][] star = {{0, 5, 5}, {1, 4, 6}, {2, 4, 6}, {3, 0, 10}, {4, 1, 9}, {5, 2, 8}, {6, 3, 7}, {7, 3, 4}, {7, 6, 7}, {8, 2, 3}, {8, 7, 8}};
                int x0 = ix - 16, y0 = iy - 13;
                for (int[] r : star) c.fill(x0 + r[1] * s + 1, y0 + r[0] * s + 1, x0 + (r[2] + 1) * s + 1, y0 + (r[0] + 1) * s + 1, 0xFF9A6A00);
                for (int[] r : star) c.fill(x0 + r[1] * s, y0 + r[0] * s, x0 + (r[2] + 1) * s, y0 + (r[0] + 1) * s, 0xFFFFC21A);
                break;
            }
            case SYM_DIAMOND: {
                int s = 3;
                int[] hw = {1, 2, 3, 4, 3, 2, 1};
                for (int r = 0; r < 7; r++) {
                    c.fill(ix - hw[r] * s + 1, iy - 11 + r * s + 1, ix + hw[r] * s + 1, iy - 11 + (r + 1) * s + 1, 0xFF0B5F86);
                }
                for (int r = 0; r < 7; r++) {
                    c.fill(ix - hw[r] * s, iy - 11 + r * s, ix + hw[r] * s, iy - 11 + (r + 1) * s, 0xFF39C5F0);
                }
                c.fill(ix - 5, iy - 8, ix - 2, iy - 2, 0xFFD8F6FF);
                break;
            }
            case SYM_BAR:
                Fx.panel(c, ix - 20, iy - 9, 40, 18, 0xFF111111, 0xFFEEEEEE);
                Fx.text(c, "BAR", cx, cy - 6f, 1.3f, 0xFFFFFFFF, true);
                break;
            default: { // cherry
                Fx.line(c, ix - 8, iy + 4, ix, iy - 12, 1, 0xFF2F8F3A);
                Fx.line(c, ix + 8, iy + 6, ix, iy - 12, 1, 0xFF2F8F3A);
                Fx.ellipse(c, ix + 6, iy - 12, 6f, 3f, 0xFF3FBF4A);
                Fx.disc(c, ix - 8, iy + 7, 7f, 0xFFD81E2C);
                Fx.disc(c, ix + 8, iy + 9, 7f, 0xFFD81E2C);
                Fx.dot(c, ix - 10, iy + 4, 2, 0xFFFFFFFF);
                Fx.dot(c, ix + 6, iy + 6, 2, 0xFFFFFFFF);
            }
        }
    }

    // =====================================================================
    // $1B NUKE
    // =====================================================================
    // stem, cap, core glow, title a, title b
    static final int[][] NUKE_PAL = {
            {0x8C6A4A, 0x5A5048, 0xFF9A3C, 0xFFD34D, 0xFF7A1A},
            {0x3FB84A, 0x2E7D32, 0x9BFF6B, 0xB6FF6B, 0x39E04A},
            {0x6A5CFF, 0x283593, 0x6FD0FF, 0xC9B8FF, 0x6FA8FF},
            {0xB3261E, 0x5A0F0A, 0xFF5A2A, 0xFFB08A, 0xFF3B2A}
    };

    static void nuke(DrawContext c, Alert a, int w, int h, float t, float d) {
        int[] pal = NUKE_PAL[a.variation % 4];
        final float impact = 2.3f;
        float tx = w / 2f, ty = h * 0.68f;

        if (t < impact) {
            float p = t / impact;
            float pulse = 0.5f + 0.5f * Fx.sin(t * 9f);
            Fx.vignette(c, w, h, 0xE01818, (0.25f + 0.30f * pulse) * Fx.smooth(t / 0.4f), (int) (w * 0.14f));
            boolean on = t < 0.5f || ((int) (t * 4f)) % 2 == 0;
            Fx.text(c, "\u26A0 NUKE INCOMING \u26A0", w / 2f, 14f, 2f, Fx.col(0xFF3B3B, on ? 1f : 0.65f), true);
            int wf = Fx.textWidth("from ");
            int wn = Fx.textWidth(a.player);
            int x0 = w / 2 - (wf + wn) / 2;
            c.drawText(Fx.tr(), "from ", x0, 36, 0xFFDDDDDD, true);
            c.drawText(Fx.tr(), a.player, x0 + wf, 36, 0xFFFFD34D, true);

            float rp = 1f + 0.12f * Fx.sin(t * 10f);
            int red = Fx.col(0xFF3B3B, 0.9f);
            Fx.ring(c, tx, ty, 26f * rp, 8f * rp, 1, red);
            Fx.ring(c, tx, ty, 12f * rp, 3.5f * rp, 1, red);
            Fx.line(c, tx - 40f, ty, tx + 40f, ty, 1, red);
            Fx.line(c, tx, ty - 12f, tx, ty + 12f, 1, red);

            float by = Fx.lerp(-30f, ty - 14f, Fx.easeIn(p));
            for (int k = 1; k <= 10; k++) {
                Fx.dot(c, tx + Fx.sin(k * 1.7f) * 1.5f, by - 14f - k * 7f, 3, Fx.col(0xFFFFFF, 0.5f * (1f - k / 10f)));
            }
            bomb(c, tx, by, pal[2]);
        } else {
            float u = t - impact;
            float cf = Fx.smooth((d - t) / 1.4f);
            var m = c.getMatrices();
            m.pushMatrix();
            float sk = u < 1.6f ? (1.6f - u) / 1.6f * 4f : 0f;
            m.translate(Fx.sin(t * 90f) * sk, Fx.cos(t * 77f) * sk);

            if (u < 1.4f) {
                float rr = w * 0.7f * Fx.easeOut(u / 1.4f);
                Fx.ring(c, tx, ty, rr, rr * 0.14f, 2, Fx.col(0xFFFFFF, 0.7f * (1f - u / 1.4f)));
            }
            float grow = Fx.easeOut(u / 3.4f);
            Fx.ellipse(c, tx, ty + 2f, 20f + 90f * grow, 6f + 10f * grow, Fx.col(0xB59A7A, 0.32f * cf));
            float stemH = h * 0.42f * grow;
            float capR = 14f + h * 0.10f * grow;
            for (float s = 0f; s < stemH; s += 5f) {
                float k = s / Math.max(1f, stemH);
                float rr = 7f + 5f * (1f - k) + Fx.sin(s * 0.2f + t * 2f) * 1.5f;
                Fx.disc(c, tx + Fx.sin(s * 0.08f + t) * 3f, ty - s, rr, Fx.col(pal[0], 0.55f * cf));
            }
            float capY = ty - stemH;
            for (int j = 0; j < 14; j++) {
                float ang = (float) (j * 2.0 * Math.PI / 14.0);
                float rr = capR * (0.5f + 0.15f * Fx.r(a.seed, j, 40));
                Fx.disc(c, tx + Fx.cos(ang) * capR * 1.25f, capY + Fx.sin(ang) * capR * 0.7f, rr, Fx.col(pal[1], 0.5f * cf));
            }
            Fx.disc(c, tx, capY, capR * 0.85f, Fx.col(pal[1], 0.5f * cf));
            float coreA = (1f - Fx.smooth(u / 4f)) * cf;
            Fx.glow(c, tx, capY + capR * 0.2f, capR * 0.7f, pal[2], 0.55f * coreA, 5);
            Fx.glow(c, tx, ty - 6f, 24f + 30f * (1f - Fx.smooth(u / 2f)), pal[2], 0.5f * coreA, 4);
            m.popMatrix();

            if (u < 1.1f) {
                float f = 1f - u / 1.1f;
                Fx.overlay(c, w, h, 0xFFFFFF, f * f * 0.95f);
            }
            float ta = Fx.smooth((u - 0.3f) / 0.5f) * Fx.smooth((d - t) / 0.9f);
            Fx.textFn(c, a.player, w / 2f, h * 0.12f, 2.4f, Fx.grad(pal[3], pal[4]), ta, true);
            Fx.text(c, "dropped a " + a.amountText + " nuke on you", w / 2f, h * 0.12f + 26f, 1f, Fx.col(0xE8ECF4, ta), false);
        }
    }

    private static void bomb(DrawContext c, float x, float y, int glowRgb) {
        int ix = Math.round(x), iy = Math.round(y);
        Fx.glow(c, x, y, 20f, glowRgb, 0.25f, 3);
        c.fill(ix - 9, iy - 16, ix - 4, iy - 6, 0xFF3C4528);
        c.fill(ix + 4, iy - 16, ix + 9, iy - 6, 0xFF3C4528);
        c.fill(ix - 3, iy - 18, ix + 3, iy - 8, 0xFF6D7A45);
        Fx.ellipse(c, x, y, 6f, 12f, 0xFF56603A);
        Fx.ellipse(c, x, y + 10f, 3f, 4f, 0xFF3C4528);
        Fx.disc(c, x, y + 1f, 3f, 0xFFFFD34D);
        Fx.disc(c, x, y + 1f, 1f, 0xFF222222);
    }

    // =====================================================================
    // $5B ORBITAL LASER
    // =====================================================================
    static final int[][] LASER_PAL = {
            {0x5EEAFF, 0xE6FDFF}, {0xFF2D2D, 0xFFC9C9}, {0xC77DFF, 0xF3E1FF}, {0xFFC83C, 0xFFF3C4}, {0x6BFF8E, 0xE2FFE9}
    };

    static void laser(DrawContext c, Alert a, int w, int h, float t, float d) {
        int v = a.variation % 5;
        int main = LASER_PAL[v][0], hot = LASER_PAL[v][1];
        float cx = w / 2f, ty = h * 0.66f;
        float satY = 10f - 34f * (1f - Fx.easeOut(t / 0.6f));
        final float beamStart = 1.6f, beamEnd = 4.8f;

        satellite(c, cx, satY, main);

        float ta = Fx.smooth((t - 0.4f) / 0.5f) * Fx.smooth((d - t - 0.2f) / 0.8f);
        if (v == 1) {
            Fx.text(c, "\u26A0 ORBITAL STRIKE INBOUND \u26A0", cx, h * 0.2f, 1.8f, Fx.col(0xFFFFFF, ta), true);
            String lock = "TARGET LOCKED - ";
            int w1 = Fx.textWidth(lock), w2 = Fx.textWidth(a.player);
            int x0 = (int) (cx - (w1 + w2) / 2f);
            int yy = (int) (h * 0.2f + 20f);
            if (ta > 0.03f) {
                c.drawText(Fx.tr(), lock, x0, yy, Fx.col(0xFF3B3B, ta), true);
                c.drawText(Fx.tr(), a.player, x0 + w1, yy, Fx.col(0xFFD34D, ta), true);
            }
        } else {
            Fx.card(c, w, (int) (h * 0.17f), a.player, "fired a " + a.amountText + " orbital laser at you", ta, Fx.grad(hot, main), main, 2.0f);
        }

        // targeting reticle
        if (t < beamStart + 0.3f) {
            float rp = 1f + 0.1f * Fx.sin(t * 9f);
            int rc = Fx.col(main, 0.9f);
            Fx.ring(c, cx, ty, 24f * rp, 7f * rp, 1, rc);
            Fx.ring(c, cx, ty, 10f * rp, 3f * rp, 1, rc);
            Fx.line(c, cx - 34f, ty, cx + 34f, ty, 1, rc);
            if (v == 1 && t > 0.9f) Fx.line(c, cx, satY + 12f, cx, ty, 1, Fx.col(main, 0.9f));
        }

        if (t >= beamStart && t < beamEnd + 0.5f) {
            float bu = t - beamStart;
            float grow = Fx.easeOut(bu / 0.2f);
            float fade = Fx.smooth((beamEnd - t) / 0.5f);
            int top = (int) (satY + 12f);
            int[] wd = {26, 18, 11, 6};
            float[] aa = {0.10f, 0.16f, 0.28f, 0.50f};
            for (int i = 0; i < 4; i++) {
                float half = wd[i] / 2f * grow * (1f + 0.1f * Fx.sin(t * 40f + i));
                c.fill(Math.round(cx - half), top, Math.round(cx + half), (int) ty, Fx.col(main, aa[i] * fade));
            }
            float ch = 1.5f * grow;
            c.fill(Math.round(cx - ch), top, Math.round(cx + ch), (int) ty, Fx.col(hot, 0.95f * fade));
            for (int k = 0; k < 4; k++) {
                float yy = top + (bu * 140f + k * 45f) % (ty - top);
                Fx.ring(c, cx, yy, 18f, 4f, 1, Fx.col(hot, 0.5f * fade));
            }
            Fx.glow(c, cx, ty, 30f + 6f * Fx.sin(t * 30f), hot, 0.7f * fade, 5);
            for (int k = 0; k < 2; k++) {
                float ph = (bu + k * 0.5f) % 1.0f;
                float rr = 150f * Fx.easeOut(ph);
                Fx.ring(c, cx, ty, rr, rr * 0.28f, 2, Fx.col(main, 0.7f * (1f - ph) * fade));
            }
            for (int i = 0; i < 24; i++) {
                float ang = Fx.r(a.seed, i, 51) * 6.28f;
                float sp = 30f + Fx.r(a.seed, i, 52) * 70f;
                float s = (bu + i * 0.05f) % 0.9f;
                Fx.dot(c, cx + Fx.cos(ang) * sp * s, ty - Math.abs(Fx.sin(ang)) * sp * s + 90f * s * s, 2, Fx.col(hot, (1f - s / 0.9f) * fade));
            }
            if (bu < 0.5f) Fx.overlay(c, w, h, main, 0.45f * (1f - bu / 0.5f));
            if (bu < 1.0f) {
                float br = w * 0.6f * Fx.easeOut(bu);
                Fx.ring(c, cx, ty, br, br, 2, Fx.col(0xFFFFFF, 0.6f * (1f - bu)));
            }
        }
        // smoke
        for (int k = 0; k < 6; k++) {
            float su = t - (beamStart + 1.0f + k * 0.3f);
            if (su <= 0f) continue;
            Fx.disc(c, cx + (Fx.r(a.seed, k, 50) - 0.5f) * 30f + Fx.sin(su + k) * 4f, ty - su * 16f - k * 3f, 9f + su * 3f,
                    Fx.col(0x8A8F99, 0.35f * Fx.smooth((d - t) / 1.2f)));
        }
    }

    private static void satellite(DrawContext c, float cx, float y, int main) {
        int x = Math.round(cx), iy = Math.round(y);
        c.fill(x - 30, iy + 2, x - 8, iy + 8, 0xFF2B5CB0);
        c.fill(x + 8, iy + 2, x + 30, iy + 8, 0xFF2B5CB0);
        for (int i = 0; i < 6; i++) {
            c.fill(x - 30 + i * 4, iy + 2, x - 29 + i * 4, iy + 8, 0xFF6FA0F0);
            c.fill(x + 8 + i * 4, iy + 2, x + 9 + i * 4, iy + 8, 0xFF6FA0F0);
        }
        c.fill(x - 8, iy + 4, x - 6, iy + 6, 0xFF888888);
        c.fill(x + 6, iy + 4, x + 8, iy + 6, 0xFF888888);
        c.fill(x - 6, iy, x + 6, iy + 9, 0xFFC8CDD6);
        c.fill(x - 3, iy - 3, x + 3, iy, 0xFFE5E8EE);
        c.fill(x, iy - 8, x + 1, iy - 3, 0xFF999999);
        Fx.dot(c, x, iy - 8, 2, 0xFFFF4040);
        Fx.dot(c, x, iy + 10, 4, Fx.col(main, 0.95f));
    }

    // =====================================================================
    // $10B BLACK HOLE
    // =====================================================================
    // disk outer, disk inner, tint, sky glow, name a, name b
    static final int[][] BH_PAL = {
            {0xFF6A2A, 0xFFF2C0, 0x3A1060, 0xFF7AD9, 0xFF7AD9, 0x7AD9FF},
            {0x3A7BFF, 0xE0F4FF, 0x0A1A5A, 0x7AA2FF, 0x7AA2FF, 0xC77DFF},
            {0xFF3A1E, 0xFFD0A0, 0x4A0A10, 0xFF6A4A, 0xFF5A5A, 0xFFC27A},
            {0x2EE07A, 0xE8FFE0, 0x063A20, 0x7AFFB0, 0x7AFFB0, 0x7A9CFF}
    };

    static void blackhole(DrawContext c, Alert a, int w, int h, float t, float d) {
        int[] p = BH_PAL[a.variation % 4];
        final float tI = 5.0f;
        float hx = w / 2f, hy = h * 0.36f;
        float grow = 0.4f + 0.6f * Fx.easeOut(t / 2f);
        float implode = 1f - Fx.easeIn((t - tI) / 0.5f);
        float R = h * 0.08f * grow * Math.max(0f, implode);
        float phaseB = t - (tI + 0.4f);
        float dark = Fx.smooth(t / 0.8f) * (phaseB < 0f ? 1f : 1f - 0.7f * Fx.smooth(phaseB / 1.5f)) * Fx.smooth((d - t) / 1.2f);
        Fx.overlay(c, w, h, p[2], 0.6f * dark);
        Fx.vignette(c, w, h, 0x000000, 0.55f * dark, (int) (w * 0.2f));

        if (R > 1f) {
            // stars being pulled in
            for (int i = 0; i < 50; i++) {
                float tt = (t * 0.35f + Fx.r(a.seed, i, 60)) % 1f;
                float rad = (1f - tt) * w * 0.5f + R;
                float ang = Fx.r(a.seed, i, 61) * 6.28f + tt * 3f;
                Fx.dot(c, hx + Fx.cos(ang) * rad, hy + Fx.sin(ang) * rad * 0.6f, 1, Fx.col(0xFFFFFF, tt * (1f - 0.3f * tt) * dark));
            }
            Fx.glow(c, hx, hy, R * 3f, p[0], 0.25f * dark, 6);
            disk(c, a, p, hx, hy, R, t, false);
            Fx.disc(c, hx, hy, R, 0xFF000000);
            for (int i = 0; i < 80; i++) {
                float ang = (float) (Math.PI + i / 79.0 * Math.PI);
                Fx.dot(c, hx + Fx.cos(ang) * R * 1.2f, hy + Fx.sin(ang) * R * 1.2f, 2, Fx.col(p[1], 0.55f));
                Fx.dot(c, hx + Fx.cos(ang) * R * 1.32f, hy + Fx.sin(ang) * R * 1.32f, 1, Fx.col(p[0], 0.35f));
            }
            Fx.ring(c, hx, hy, R * 1.04f, R * 1.04f, 1, Fx.col(p[1], 0.9f));
            disk(c, a, p, hx, hy, R, t, true);
        }

        if (phaseB > 0f) {
            float flash = 1f - Fx.smooth(phaseB / 1.4f);
            Fx.overlay(c, w, h, 0xFFFFFF, flash * flash * 0.9f);
            float gl = Fx.easeOut(phaseB / 2f) * Fx.smooth((d - t) / 1.5f);
            for (int i = 8; i >= 1; i--) {
                Fx.disc(c, hx, hy, w * 0.07f * i * gl + 4f, Fx.col(p[3], 0.045f));
            }
            float sa = Fx.smooth(phaseB / 1.0f) * Fx.smooth((d - t) / 1.2f);
            for (int i = 0; i < 90; i++) {
                float tw = 0.5f + 0.5f * Fx.sin(t * 3f + i);
                Fx.dot(c, Fx.r(a.seed, i, 62) * w, Fx.r(a.seed, i, 63) * h * 0.85f, (i % 4 == 0) ? 2 : 1, Fx.col(0xFFFFFF, tw * sa));
            }
            float ta = Fx.smooth((phaseB - 0.2f) / 0.6f) * Fx.smooth((d - t) / 0.9f);
            Fx.textFn(c, a.player, w / 2f, h * 0.14f, 2.4f, Fx.grad(p[4], p[5]), ta, true);
            Fx.text(c, "paid you " + a.amountText, w / 2f, h * 0.14f + 26f, 1.3f, Fx.col(0xFFFFFF, ta), true);
            Fx.text(c, "and created a new universe", w / 2f, h * 0.14f + 42f, 0.8f, Fx.col(0xC8CCD8, ta), false);
        }
    }

    private static void disk(DrawContext c, Alert a, int[] p, float hx, float hy, float R, float t, boolean front) {
        for (int i = 0; i < 240; i++) {
            float rr = 1.25f + 1.6f * Fx.r(a.seed, i, 0);
            float a0 = Fx.r(a.seed, i, 1) * 6.28f;
            float omega = 2.6f / (rr * Fx.sqrt(rr));
            float ang = a0 + t * omega * 1.4f;
            float s = Fx.sin(ang);
            if ((s > 0f) != front) continue;
            float k = (rr - 1.25f) / 1.6f;
            int color = Fx.col(Fx.mix(p[1], p[0], k), 0.9f - 0.35f * k);
            Fx.dot(c, hx + Fx.cos(ang) * R * rr, hy + s * R * rr * 0.26f, rr < 1.9f ? 2 : 1, color);
        }
    }

    // =====================================================================
    // $100B GOD TIER
    // =====================================================================
    // tint, rays, crown, title
    static final int[][] GOD_PAL = {
            {0x6B5200, 0xFFE08A, 0xFFD34D, 0xFFD34D},
            {0x0E5A70, 0xA8F4FF, 0x5EEAFF, 0x5EEAFF},
            {0x0B5A2A, 0xA8FFC4, 0x2ECC71, 0x7DFFAE},
            {0x2A2036, 0xE0D4FF, 0x4A4458, 0xD8D0E8}
    };
    static final String[] GOD_TITLE = {"GOD TIER", "DIAMOND GOD", "EMERALD EMPEROR", "NETHERITE KING"};

    static void god(DrawContext c, Alert a, int w, int h, float t, float d) {
        int v = a.variation % 4;
        int[] p = GOD_PAL[v];
        float env = Fx.env(t, d, 0.8f, 1.2f);
        int cx = w / 2;
        int cy = (int) (h * 0.42f);

        Fx.overlay(c, w, h, p[0], 0.6f * env);
        Fx.overlay(c, w, h, 0x000000, 0.25f * env);
        Fx.glow(c, cx, cy + 10f, h * 0.28f, p[1], 0.35f * env, 8);

        // god rays
        for (int i = 0; i < 22; i++) {
            float ang = (float) (i * 2.0 * Math.PI / 22.0) + t * 0.12f;
            float cs = Fx.cos(ang), sn = Fx.sin(ang);
            float s = 20f;
            while (s < w * 0.9f) {
                float size = Math.max(3f, 2f + s * 0.05f);
                Fx.dot(c, cx + cs * s, cy + sn * s, (int) size, Fx.col(p[1], 0.07f * env));
                s += size * 0.8f;
            }
        }

        // pedestal + aura
        Fx.ellipse(c, cx, cy + 50f, 62f, 10f, Fx.col(p[1], 0.30f * env));
        Fx.ring(c, cx, cy + 50f, 62f, 10f, 1, Fx.col(p[1], 0.7f * env));
        Fx.ring(c, cx, cy + 4f, 50f + 6f * Fx.sin(t * 2f), 14f, 1, Fx.col(p[1], 0.45f * env));

        // head + crown
        float bob = Fx.sin(t * 1.6f) * 2f;
        int headY = (int) (cy - 20 + bob);
        Fx.head(c, a.player, cx - 26, headY, 52);
        int bx = cx - 22, by = headY - 6;
        int[] jewels = {0xFF4D4D, 0x4DA3FF, 0x6BFF8E, 0xFFD34D};
        int crown = 0xFF000000 | p[2];
        c.fill(bx, by, bx + 44, by + 7, crown);
        c.fill(bx, by + 5, bx + 44, by + 7, 0xFF000000 | Fx.mix(p[2], 0x000000, 0.35f));
        for (int i = 0; i < 5; i++) {
            int sx = bx + i * 10;
            c.fill(sx, by - 9, sx + 6, by, crown);
            c.fill(sx + 2, by - 13, sx + 4, by - 9, crown);
            Fx.dot(c, sx + 3, by - 14, 3, 0xFF000000 | jewels[i % 4]);
        }
        for (int i = 0; i < 4; i++) {
            Fx.dot(c, bx + 8 + i * 10, by + 3, 3, 0xFF000000 | jewels[(i + 1) % 4]);
        }

        // titles
        float ta = Fx.smooth((t - 0.5f) / 0.6f) * Fx.smooth((d - t) / 1.0f);
        Fx.text(c, "\u265B " + GOD_TITLE[v] + " \u265B", w / 2f, h * 0.06f, 2.0f, Fx.col(p[3], ta), true);
        final float tt = t;
        Fx.textFn(c, a.player, w / 2f, h * 0.06f + 24f, 1.4f, (i, n) -> Fx.hsv(i / (float) Math.max(1, n) * 0.8f + tt * 0.2f, 0.5f, 1f), ta, true);
        Fx.text(c, "paid you " + a.amountText, w / 2f, h * 0.06f + 40f, 1f, Fx.col(0xE8ECF4, ta), false);

        // confetti
        for (int i = 0; i < 160; i++) {
            float x = Fx.r(a.seed, i, 70) * w + Fx.sin(t * 2f + i) * 10f;
            float y = (t * (40f + Fx.r(a.seed, i, 71) * 50f) + Fx.r(a.seed, i, 72) * h) % (h + 20f) - 10f;
            int color = Fx.hsv(Fx.r(a.seed, i, 73), 0.8f, 1f);
            boolean flip = ((int) (t * 6f) + i) % 2 == 0;
            c.fill(Math.round(x), Math.round(y), Math.round(x) + (flip ? 2 : 3), Math.round(y) + (flip ? 3 : 2), Fx.col(color, 0.9f * env));
        }
    }
}
