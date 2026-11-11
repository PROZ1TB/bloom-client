package com.paymentalerts;

import net.minecraft.client.gui.DrawContext;

/** Dispatcher + the simpler effects (toast, money rain, fireworks, lightning). */
final class Effects {
    private Effects() {}

    record Snd(float time, String id, float pitch, float vol) {}

    static Snd[] sounds(Tier t) {
        switch (t) {
            case SKIN: return new Snd[]{ new Snd(0f, "entity.experience_orb.pickup", 1.2f, 0.8f) };
            case MONEY: return new Snd[]{ new Snd(0f, "entity.player.levelup", 1.3f, 0.7f), new Snd(0.3f, "entity.experience_orb.pickup", 1.0f, 0.8f) };
            case FIREWORKS: return new Snd[]{ new Snd(0.4f, "entity.firework_rocket.launch", 1f, 0.8f), new Snd(1.2f, "entity.firework_rocket.large_blast", 1f, 0.9f), new Snd(2.2f, "entity.firework_rocket.twinkle", 1f, 0.9f), new Snd(3.4f, "entity.firework_rocket.large_blast", 1.1f, 0.9f), new Snd(5.0f, "entity.firework_rocket.large_blast", 0.9f, 0.9f) };
            case LIGHTNING: return new Snd[]{ new Snd(0.7f, "entity.lightning_bolt.thunder", 1f, 1f), new Snd(2.05f, "entity.lightning_bolt.thunder", 0.9f, 1f), new Snd(3.4f, "entity.lightning_bolt.thunder", 1.1f, 1f), new Snd(4.75f, "entity.lightning_bolt.thunder", 0.8f, 1f) };
            case JACKPOT: return new Snd[]{ new Snd(0.2f, "block.note_block.bell", 1f, 1f), new Snd(1.2f, "block.note_block.hat", 1.4f, 0.6f), new Snd(3.75f, "ui.toast.challenge_complete", 1f, 1f), new Snd(3.8f, "entity.player.levelup", 1f, 1f) };
            case NUKE: return new Snd[]{ new Snd(0f, "block.note_block.bit", 0.6f, 1f), new Snd(0.5f, "block.note_block.bit", 0.6f, 1f), new Snd(1.0f, "block.note_block.bit", 0.6f, 1f), new Snd(1.5f, "block.note_block.bit", 0.6f, 1f), new Snd(2.3f, "entity.generic.explode", 0.6f, 1f), new Snd(2.4f, "entity.warden.sonic_boom", 0.7f, 1f) };
            case LASER: return new Snd[]{ new Snd(0.2f, "block.beacon.activate", 1f, 1f), new Snd(1.6f, "entity.warden.sonic_boom", 1.2f, 1f), new Snd(1.7f, "block.beacon.power_select", 0.8f, 1f), new Snd(2.6f, "entity.generic.explode", 1f, 0.8f) };
            case BLACKHOLE: return new Snd[]{ new Snd(0.2f, "block.portal.ambient", 0.6f, 1f), new Snd(4.9f, "entity.generic.explode", 0.5f, 1f), new Snd(5.0f, "block.end_portal.spawn", 1f, 1f) };
            case GOD: return new Snd[]{ new Snd(0.1f, "block.beacon.activate", 1.2f, 1f), new Snd(0.4f, "ui.toast.challenge_complete", 1f, 1f), new Snd(1.0f, "entity.player.levelup", 0.8f, 1f), new Snd(2.0f, "entity.firework_rocket.twinkle", 1f, 1f) };
            default: return new Snd[0];
        }
    }

    static void render(DrawContext c, Alert a, int w, int h, float t) {
        float d = a.tier.duration;
        switch (a.tier) {
            case MONEY: money(c, a, w, h, t, d); break;
            case FIREWORKS: fireworksTier(c, a, w, h, t, d); break;
            case LIGHTNING: lightning(c, a, w, h, t, d); break;
            case JACKPOT: BigEffects.jackpot(c, a, w, h, t, d); break;
            case NUKE: BigEffects.nuke(c, a, w, h, t, d); break;
            case LASER: BigEffects.laser(c, a, w, h, t, d); break;
            case BLACKHOLE: BigEffects.blackhole(c, a, w, h, t, d); break;
            case GOD: BigEffects.god(c, a, w, h, t, d); break;
            default: break;
        }
    }

    // =====================================================================
    // Toast (skin tab / gold tab)
    // =====================================================================
    static final int[] TOAST_BORDERS = { 0x4ADE80, 0xB36BFF, 0x5EEAFF, 0xFFD34D };

    static void toast(DrawContext c, Alert a, int w, int h, int index, float age, float dur) {
        int tw = 128, th = 28;
        float in = Fx.easeOut(age / 0.35f);
        float out = Fx.easeIn((age - (dur - 0.4f)) / 0.4f);
        float off = in * (1f - out);
        if (off <= 0.01f) return;
        int x = Math.round(w + 10 - (tw + 16) * off);
        int y = 6 + index * (th + 4);
        int border = a.tier == Tier.MONEY ? 0xFFC83C : TOAST_BORDERS[a.variation % TOAST_BORDERS.length];
        Fx.panel(c, x, y, tw, th, 0xEE141A2B, 0xFF000000 | border);

        // head "crouch dance" (4 styles = 4 variations)
        float crouch = crouchAmount(a.variation, age);
        var m = c.getMatrices();
        m.pushMatrix();
        m.translate(x + 5, y + 5 + 18);
        m.scale(1f, 1f - 0.16f * crouch);
        Fx.head(c, a.player, 0, -18, 18);
        m.popMatrix();

        c.drawText(Fx.tr(), a.player, x + 30, y + 6, 0xFFFFD34D, true);
        c.drawText(Fx.tr(), "paid " + a.amountText, x + 30, y + 16, 0xFF9CF0B0, true);

        float left = Fx.clamp01(1f - age / dur);
        Fx.rect(c, x + 1, y + th - 2, (int) ((tw - 2) * left), 1, 0xFF000000 | border);
    }

    /** 0..1 how "crouched" the head is. */
    static float crouchAmount(int variation, float t) {
        switch (variation % 4) {
            case 0: return ((int) (t / 0.30f)) % 2 == 0 ? 0f : 1f;          // steady
            case 1: return ((int) (t / 0.14f)) % 2 == 0 ? 0f : 1f;          // fast
            case 2: {                                                        // double
                float p = t % 1.0f;
                return (p < 0.12f || (p > 0.24f && p < 0.36f)) ? 1f : 0f;
            }
            default: return ((int) (t / 0.80f)) % 2 == 0 ? 0f : 1f;         // slow
        }
    }

    // =====================================================================
    // Money rain
    // =====================================================================
    static void money(DrawContext c, Alert a, int w, int h, float t, float d) {
        float al = Fx.env(t, d, 0.3f, 1.3f);
        rain(c, a.seed, w, h, t, 0f, d - 3.2f, a.variation, 110, al);
    }

    /** variation: 0 bills, 1 gold bars, 2 diamonds+emeralds, 3 mixed, 4 gold coins */
    static void rain(DrawContext c, long seed, int w, int h, float t, float start, float window, int variation, int n, float al) {
        if (al <= 0.01f) return;
        for (int i = 0; i < n; i++) {
            float spawn = start + Fx.r(seed, i, 0) * Math.max(0.5f, window);
            float tt = t - spawn;
            if (tt < 0f) continue;
            float speed = 55f + Fx.r(seed, i, 1) * 80f;
            float cycle = h + 30f;
            float y = (tt * speed) % cycle - 15f;
            float x = Fx.r(seed, i, 2) * w + Fx.sin(tt * 1.7f + i) * 9f;
            float ph = Fx.r(seed, i, 3) * 6.28f;
            int kind;
            switch (variation) {
                case 0: kind = 0; break;
                case 1: kind = 1; break;
                case 2: kind = 2 + (i % 2); break;
                case 4: kind = 4; break;
                default: { int[] mixed = {0, 1, 2, 3, 4}; kind = mixed[i % 5]; }
            }
            item(c, kind, x, y, tt, ph, al);
        }
    }

    private static void item(DrawContext c, int kind, float x, float y, float tt, float ph, float al) {
        float sq = Math.abs(Fx.cos(tt * 3f + ph));
        int ix = Math.round(x), iy = Math.round(y);
        switch (kind) {
            case 0: { // bill
                int bw = 3 + (int) (7 * sq);
                c.fill(ix, iy, ix + bw, iy + 5, Fx.col(0x1B6B38, al));
                c.fill(ix + 1, iy + 1, ix + bw - 1, iy + 4, Fx.col(0x5ED480, al));
                if (bw > 5) c.fill(ix + bw / 2 - 1, iy + 2, ix + bw / 2 + 1, iy + 3, Fx.col(0xD9FFE4, al));
                break;
            }
            case 1: { // gold bar
                int bw = 6 + (int) (4 * sq);
                c.fill(ix, iy, ix + bw, iy + 5, Fx.col(0xA9740B, al));
                c.fill(ix + 1, iy + 1, ix + bw - 1, iy + 4, Fx.col(0xFFC107, al));
                c.fill(ix + 1, iy + 1, ix + bw - 1, iy + 2, Fx.col(0xFFF0A0, al));
                break;
            }
            case 2: { // diamond gem
                int s = 2;
                int[] hw = {1, 2, 3, 2, 1};
                for (int r = 0; r < 5; r++) {
                    int half = Math.max(1, Math.round(hw[r] * (0.5f + 0.5f * sq) * 1.5f));
                    c.fill(ix - half, iy + r * s, ix + half, iy + (r + 1) * s, Fx.col(0x4DEEEA, al));
                }
                c.fill(ix - 1, iy + 2, ix + 1, iy + 4, Fx.col(0xFFFFFF, al * 0.9f));
                break;
            }
            case 3: { // emerald
                int bw = 3 + (int) (3 * sq);
                c.fill(ix, iy, ix + bw, iy + 7, Fx.col(0x1E9E55, al));
                c.fill(ix + 1, iy + 1, ix + bw - 1, iy + 6, Fx.col(0x3EE58A, al));
                c.fill(ix + 1, iy + 1, ix + 2, iy + 3, Fx.col(0xD6FFE8, al));
                break;
            }
            default: { // gold coin
                Fx.ellipse(c, x, y, 1.5f + 2.5f * sq, 3.5f, Fx.col(0xFFD54A, al));
                Fx.ellipse(c, x, y, 0.8f + 1.4f * sq, 2.0f, Fx.col(0xFFF3B0, al * 0.9f));
            }
        }
    }

    // =====================================================================
    // Fireworks
    // =====================================================================
    static void fireworksTier(DrawContext c, Alert a, int w, int h, float t, float d) {
        float al = Fx.env(t, d, 0.4f, 0.8f);
        int count = a.variation == 2 ? 15 : 9;
        float gap = (d - 3.2f) / count;
        fireworks(c, a.seed, w, h, t, 0.35f, gap, count, a.variation);
        Fx.ColorFn fn = a.variation == 1 ? Fx.grad(0xFFE9A0, 0xFFB300)
                : a.variation == 3 ? Fx.grad(0xFF6B9A, 0xFF2D55)
                : Fx.rainbow(t * 0.25f);
        Fx.card(c, w, 10, a.player, "paid you " + a.amountText, al, fn, 0xFFD34D, 2.2f);
    }

    /** mode: 0 classic, 1 gold, 2 rainbow finale, 3 hearts */
    static void fireworks(DrawContext c, long seed, int w, int h, float t, float first, float gap, int count, int mode) {
        final float rocket = 0.8f, life = 1.7f;
        int[] pal = {0xFF4D6D, 0x4DA3FF, 0xFFD34D, 0x6BFF8E, 0xC77DFF, 0xFFFFFF};
        int[] golds = {0xFFD34D, 0xFFB300, 0xFFF1B0, 0xFFFFFF};
        int[] hearts = {0xFF3B6B, 0xFF7AA2, 0xFF2D55, 0xFFD0DC};
        for (int i = 0; i < count; i++) {
            float tl = first + i * gap;
            float te = tl + rocket;
            if (t < tl || t > te + life) continue;
            float bx = w * (0.12f + 0.76f * Fx.r(seed, i, 1));
            float by = h * (0.10f + 0.32f * Fx.r(seed, i, 2));
            float sx = bx + (Fx.r(seed, i, 3) - 0.5f) * 40f;
            float sy = h * 0.9f;
            if (t < te) {
                float p = Fx.easeOut((t - tl) / rocket);
                for (int k = 0; k < 14; k++) {
                    float pp = p - k * 0.035f;
                    if (pp < 0f) break;
                    Fx.dot(c, Fx.lerp(sx, bx, pp), Fx.lerp(sy, by, pp), 2, Fx.col(0xFFD98A, 1f - k / 14f));
                }
                continue;
            }
            float u = t - te;
            if (u < 0.18f) Fx.disc(c, bx, by, h * 0.08f * (1f - u / 0.18f), Fx.col(0xFFFFFF, 0.5f));
            int shape;
            switch (mode) {
                case 3: shape = (i % 3 == 2) ? 0 : 2; break;
                case 1: shape = (i % 3 == 0) ? 3 : (i % 3 == 1 ? 1 : 0); break;
                default: shape = i % 4;
            }
            int burstColor = pal[(int) (Fx.r(seed, i, 4) * pal.length) % pal.length];
            float tilt = (Fx.r(seed, i, 6) - 0.5f) * 1.2f;
            int n = 56;
            float R = h * 0.15f * Fx.easeOut(u / 0.9f);
            float fade = 1f - u / life;
            float alpha = fade * fade;
            for (int j = 0; j < n; j++) {
                float ang = (float) (j * 2.0 * Math.PI / n);
                float ux, uy;
                switch (shape) {
                    case 1: {
                        float ex = Fx.cos(ang), ey = Fx.sin(ang) * 0.38f;
                        ux = ex * Fx.cos(tilt) - ey * Fx.sin(tilt);
                        uy = ex * Fx.sin(tilt) + ey * Fx.cos(tilt);
                        break;
                    }
                    case 2: {
                        float s = Fx.sin(ang);
                        float hx = 16f * s * s * s;
                        float hy = -(13f * Fx.cos(ang) - 5f * Fx.cos(2 * ang) - 2f * Fx.cos(3 * ang) - Fx.cos(4 * ang));
                        ux = hx / 17f;
                        uy = hy / 17f;
                        break;
                    }
                    case 3: {
                        float rad = 0.4f + 0.6f * (float) Math.pow(Math.abs(Math.cos(2.5 * ang)), 4);
                        ux = Fx.cos(ang) * rad;
                        uy = Fx.sin(ang) * rad;
                        break;
                    }
                    default: {
                        float rad = 0.35f + 0.65f * Fx.r(seed + i, j, 5);
                        ux = Fx.cos(ang) * rad;
                        uy = Fx.sin(ang) * rad;
                    }
                }
                if (u > 1.0f && ((int) (u * 18f) + j) % 3 == 0) continue; // twinkle out
                int color;
                switch (mode) {
                    case 1: color = golds[(j + i) % golds.length]; break;
                    case 2: color = Fx.hsv(j / (float) n + i * 0.13f, 0.75f, 1f); break;
                    case 3: color = hearts[(j + i) % hearts.length]; break;
                    default: color = (j % 7 == 0) ? 0xFFFFFF : burstColor;
                }
                float px = bx + ux * R;
                float py = by + uy * R + 16f * u * u;
                Fx.dot(c, px, py, u < 1.1f ? 2 : 1, Fx.col(color, alpha));
            }
        }
    }

    // =====================================================================
    // Lightning storm
    // =====================================================================
    static final int[] BOLT = {0x7EB6FF, 0xC58CFF, 0xFF6B6B, 0xFFD34D};
    static final int[][] NAME_GRAD = {
            {0xA8D8FF, 0x3B82F6}, {0xE2B8FF, 0x8B5CF6}, {0xFFFFFF, 0xFF3B3B}, {0xFFE9A0, 0xFFB300}
    };

    static void lightning(DrawContext c, Alert a, int w, int h, float t, float d) {
        float al = Fx.env(t, d, 0.6f, 1.0f);
        int v = a.variation % 4;
        int glow = BOLT[v];
        Fx.overlay(c, w, h, 0x070B16, 0.62f * al);
        c.fillGradient(0, 0, w, (int) (h * 0.35f), Fx.col(0x05070D, 0.8f * al), Fx.col(0x05070D, 0f));

        // rain
        for (int i = 0; i < 120; i++) {
            float x = Fx.r(a.seed, i, 20) * w;
            float sp = 380f * (0.8f + 0.4f * Fx.r(a.seed, i, 21));
            float y = (t * sp + Fx.r(a.seed, i, 22) * h) % (h + 20f) - 10f;
            c.fill(Math.round(x), Math.round(y), Math.round(x) + 1, Math.round(y) + 7, Fx.col(0xAFC8FF, 0.38f * al));
        }

        // strikes
        for (int k = 0; k < 5; k++) {
            float ts = 0.7f + k * 1.35f;
            float u = t - ts;
            if (u < 0f || u > 0.55f) continue;
            float x0 = w * (0.12f + 0.76f * Fx.r(a.seed, k, 10));
            float gy = h * (0.62f + 0.08f * Fx.r(a.seed, k, 11));
            if (u < 0.22f) Fx.overlay(c, w, h, Fx.mix(0xFFFFFF, glow, 0.35f), 0.42f * (1f - u / 0.22f) * al);
            boolean on = u < 0.08f || (u > 0.14f && u < 0.24f) || (u > 0.3f && u < 0.42f);
            if (!on) continue;
            int segs = 16;
            float[] px = new float[segs + 1];
            float[] py = new float[segs + 1];
            float x = x0;
            for (int i = 0; i <= segs; i++) {
                py[i] = gy * i / segs;
                x += (Fx.r(a.seed, k * 40 + i, 12) - 0.5f) * 26f;
                px[i] = x;
            }
            for (int i = 0; i < segs; i++) {
                Fx.line(c, px[i], py[i], px[i + 1], py[i + 1], 6, Fx.col(glow, 0.22f * al));
                Fx.line(c, px[i], py[i], px[i + 1], py[i + 1], 3, Fx.col(glow, 0.55f * al));
                Fx.line(c, px[i], py[i], px[i + 1], py[i + 1], 2, Fx.col(0xFFFFFF, 0.95f * al));
            }
            for (int b = 0; b < 3; b++) {
                int s = 3 + (int) (Fx.r(a.seed, k * 7 + b, 13) * 9f);
                float bx = px[s], by = py[s];
                float dir = Fx.r(a.seed, k * 7 + b, 14) < 0.5f ? -1f : 1f;
                for (int i = 0; i < 5; i++) {
                    float nx = bx + dir * (6f + Fx.r(a.seed, k * 90 + b * 9 + i, 15) * 8f);
                    float ny = by + 8f + Fx.r(a.seed, k * 90 + b * 9 + i, 16) * 8f;
                    Fx.line(c, bx, by, nx, ny, 2, Fx.col(glow, 0.6f * al));
                    Fx.line(c, bx, by, nx, ny, 1, Fx.col(0xFFFFFF, 0.9f * al));
                    bx = nx; by = ny;
                }
            }
            Fx.ellipse(c, px[segs], gy, 24f, 6f, Fx.col(glow, 0.35f * al));
            for (int i = 0; i < 10; i++) {
                float ang = Fx.r(a.seed, k * 11 + i, 17) * 6.28f;
                float sp = 20f + Fx.r(a.seed, k * 11 + i, 18) * 40f;
                Fx.dot(c, px[segs] + Fx.cos(ang) * sp * u * 2f, gy - Math.abs(Fx.sin(ang)) * sp * u * 2f + 80f * u * u, 1, Fx.col(0xFFFFFF, al * (1f - u / 0.55f)));
            }
        }

        // title
        float ta = Fx.smooth((t - 0.4f) / 0.5f) * Fx.smooth((d - t) / 0.9f);
        Fx.textFn(c, a.player, w / 2f, h * 0.30f, 2.4f, Fx.grad(NAME_GRAD[v][0], NAME_GRAD[v][1]), ta, true);
        Fx.text(c, "\u26A1 paid you " + a.amountText + " \u26A1", w / 2f, h * 0.30f + 26f, 1f, Fx.col(0xE8ECF4, ta), false);
    }
}
