package dev.bloom.visual;

import dev.bloom.Theme;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Random;

/**
 * Draws animated skies (aurora, nebula, swirl, golden, sunset) with plain rectangles, using a real
 * camera projection so the sky stays put in the world as you turn and look up and down.
 * Used by the sky overlay module and by the main menu.
 */
public final class SkyArt {
    public static final String[] STYLES = { "Aurora", "Nebula", "Swirl", "Golden", "Sunset" };

    private static final int STAR_COUNT = 240;
    private static final double[][] STARS = makeStars();

    private SkyArt() { }

    private static double[][] makeStars() {
        Random r = new Random(1337);
        double[][] s = new double[STAR_COUNT][4];
        for (int i = 0; i < STAR_COUNT; i++) {
            double az = r.nextDouble() * Math.PI * 2, el = Math.asin(0.03 + r.nextDouble() * 0.97);
            s[i][0] = -Math.sin(az) * Math.cos(el);
            s[i][1] = Math.sin(el);
            s[i][2] = Math.cos(az) * Math.cos(el);
            s[i][3] = r.nextDouble() * 6.283;
        }
        return s;
    }

    // ------------------------------------------------------------------ camera

    private static final class Cam {
        double fx, fy, fz, rx, rz, ux, uy, uz, tanHalf, aspect;
        int gw, gh;

        Cam(int gw, int gh, double yawDeg, double pitchDeg, double fovDeg) {
            double yaw = Math.toRadians(yawDeg), pitch = Math.toRadians(pitchDeg);
            double cy = Math.cos(yaw), sy = Math.sin(yaw), cp = Math.cos(pitch), sp = Math.sin(pitch);
            fx = -sy * cp; fy = -sp; fz = cy * cp;
            rx = -cy; rz = -sy;
            ux = -sy * sp; uy = cp; uz = cy * sp;
            tanHalf = Math.tan(Math.toRadians(Math.max(20, Math.min(130, fovDeg)) / 2));
            aspect = gw / (double) Math.max(1, gh);
            this.gw = gw;
            this.gh = gh;
        }

        /** Projects a direction given as azimuth (same convention as the player's yaw) and elevation, both in degrees. */
        double[] project(double azDeg, double elDeg) {
            double az = Math.toRadians(azDeg), el = Math.toRadians(elDeg);
            double dx = -Math.sin(az) * Math.cos(el), dy = Math.sin(el), dz = Math.cos(az) * Math.cos(el);
            return projectVec(dx, dy, dz);
        }

        double[] projectVec(double dx, double dy, double dz) {
            double zf = dx * fx + dy * fy + dz * fz;
            if (zf <= 0.03) return null;
            double xr = dx * rx + dz * rz;
            double yu = dx * ux + dy * uy + dz * uz;
            double sx = gw / 2.0 + (xr / zf) / (tanHalf * aspect) * gw / 2.0;
            double sy = gh / 2.0 - (yu / zf) / tanHalf * gh / 2.0;
            return new double[] { sx, sy };
        }
    }

    /** Screen y of the horizon line for a camera pitch (positive = looking down) and vertical FOV. */
    public static int horizonY(int gh, double pitchDeg, double fovDeg) {
        double tanHalf = Math.tan(Math.toRadians(Math.max(20, Math.min(130, fovDeg)) / 2));
        double v = gh / 2.0 - Math.tan(Math.toRadians(pitchDeg)) / tanHalf * gh / 2.0;
        return (int) Math.round(Math.max(-100000, Math.min(100000, v)));
    }

    // ------------------------------------------------------------------ helpers

    private static void box(GuiGraphics g, int gw, int gh, double x0, double y0, double x1, double y1, int argb) {
        int ax = (int) Math.max(0, Math.floor(x0)), ay = (int) Math.max(0, Math.floor(y0));
        int bx = (int) Math.min(gw, Math.ceil(x1)), by = (int) Math.min(gh, Math.ceil(y1));
        if (bx <= ax || by <= ay || (argb >>> 24) < 3) return;
        g.fill(ax, ay, bx, by, argb);
    }

    private static int mix(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static int alpha(double a, int rgb) {
        return Theme.argb((int) Math.max(0, Math.min(255, Math.round(a))), rgb);
    }

    private static double clamp01(double v) { return Math.max(0, Math.min(1, v)); }

    // ------------------------------------------------------------------ main entry

    public static void draw(GuiGraphics g, int gw, int gh, double yawDeg, double pitchDeg, double fovDeg,
                            int style, double intensity, double time, boolean stars, boolean glow, boolean veil, int accentRgb) {
        style = Math.max(0, Math.min(STYLES.length - 1, style));
        intensity = Math.max(0.05, Math.min(1.5, intensity));
        Cam cam = new Cam(gw, gh, yawDeg, pitchDeg, fovDeg);
        int horizon = horizonY(gh, pitchDeg, fovDeg);
        if (horizon < 4) return; // looking at the ground: no sky on screen

        int top = 0x9B6BFF, bottom = 0x4DFFC8, veilRgb = 0x0A0618;
        if (Theme.glass) { top = 0xBFEAFF; bottom = 0xFFFFFF; veilRgb = 0x1C3A5A; }
        else if (style == 0) { top = mix(accentRgb, 0xFF6BD6, 0.35); bottom = 0x4DFFC8; }
        else if (style == 3) { top = 0xFFB22E; bottom = 0xFFE08A; veilRgb = 0x2A1604; }
        else if (style == 4) { top = 0xFF5C8A; bottom = 0xFFA040; veilRgb = 0x1A0A24; }
        else if (style == 1) { top = 0x7A5CFF; bottom = 0xFF6BD6; veilRgb = 0x0A0A24; }
        else if (style == 2) { veilRgb = 0x000000; }

        // darkened sky tint above the horizon
        if (veil) {
            double veilStrength = style == 2 ? 0.62 : style == 3 ? 0.34 : 0.45;
            int slices = 12;
            int y0 = 0;
            for (int i = 0; i < slices; i++) {
                int y1 = (int) Math.round(horizon * (i + 1) / (double) slices);
                double f = (i + 0.5) / slices;                       // 0 top .. 1 horizon
                double a = 255 * veilStrength * intensity * (0.95 - 0.55 * f);
                int c = style == 4 ? mix(veilRgb, 0xFF6A3D, f * f * 0.8) : veilRgb;
                box(g, gw, gh, 0, y0, gw, y1, alpha(a * 0.9, c));
                y0 = y1;
            }
        }

        if (stars) drawStars(g, cam, time, intensity);

        switch (style) {
            case 1: drawNebula(g, cam, yawDeg, time, intensity, top, bottom); break;
            case 2: drawSwirl(g, cam, time, intensity); break;
            case 4: drawSunset(g, cam, horizon, intensity, time); break;
            default: drawCurtains(g, cam, yawDeg, time, intensity, top, bottom, style == 3); break;
        }

        if (glow) {
            int c = Theme.glass ? 0xFFFFFF : mix(accentRgb, 0xFFFFFF, 0.25);
            for (int k = -4; k <= 4; k++) {
                double a = 120 * intensity * (1 - Math.abs(k) / 5.0);
                box(g, gw, gh, 0, horizon + k, gw, horizon + k + 1, alpha(a, c));
            }
        }
    }

    private static void drawStars(GuiGraphics g, Cam cam, double time, double intensity) {
        for (double[] s : STARS) {
            double[] p = cam.projectVec(s[0], s[1], s[2]);
            if (p == null) continue;
            if (p[0] < 0 || p[1] < 0 || p[0] >= cam.gw || p[1] >= cam.gh) continue;
            double a = (70 + 110 * (0.5 + 0.5 * Math.sin(time * 1.7 + s[3]))) * Math.min(1.0, intensity + 0.3);
            box(g, cam.gw, cam.gh, p[0], p[1], p[0] + 1, p[1] + 1, alpha(a, 0xFFFFFF));
        }
    }

    /** Vertical light curtains (aurora and golden). */
    private static void drawCurtains(GuiGraphics g, Cam cam, double yaw, double time, double intensity,
                                     int top, int bottom, boolean wide) {
        double hfov = Math.toDegrees(2 * Math.atan(cam.tanHalf * cam.aspect));
        double range = hfov * 0.8;
        double step = Math.max(1.0, range * 2 / 100.0);
        for (double az = yaw - range; az <= yaw + range; az += step) {
            double s1 = Math.sin(az * 0.06 + time * 0.15);
            double s2 = Math.sin(az * 0.15 - time * 0.22);
            double s3 = Math.sin(az * 0.37 + time * 0.40);
            double e0 = (wide ? 9 : 16) + 6 * s1;
            double hgt = (wide ? 34 : 18) + 12 * (0.5 + 0.5 * s2);
            double power = clamp01(0.35 + 0.45 * s2 + 0.20 * s3) * intensity;
            if (power < 0.04) continue;
            double[] a = cam.project(az - step / 2, e0 + hgt * 0.5);
            double[] b = cam.project(az + step / 2, e0 + hgt * 0.5);
            double[] yt = cam.project(az, e0 + hgt);
            double[] yb = cam.project(az, e0);
            if (a == null || b == null || yt == null || yb == null) continue;
            double x0 = Math.min(a[0], b[0]), x1 = Math.max(a[0], b[0]);
            if (x1 - x0 < 1) x1 = x0 + 1;
            int slices = 6;
            for (int k = 0; k < slices; k++) {
                double f0 = k / (double) slices, f1 = (k + 1) / (double) slices;
                double ya = yt[1] + (yb[1] - yt[1]) * f0, yb2 = yt[1] + (yb[1] - yt[1]) * f1;
                double mid = (f0 + f1) / 2;
                double a1 = 255 * (wide ? 0.30 : 0.42) * power * Math.pow(Math.sin(Math.PI * Math.min(1, mid * 0.92 + 0.05)), 1.3);
                box(g, cam.gw, cam.gh, x0, ya, x1, yb2 + 1, alpha(a1, mix(top, bottom, mid)));
            }
        }
    }

    private static void drawNebula(GuiGraphics g, Cam cam, double yaw, double time, double intensity, int top, int bottom) {
        double hfov = Math.toDegrees(2 * Math.atan(cam.tanHalf * cam.aspect));
        double range = hfov * 0.8;
        double step = Math.max(2.0, range * 2 / 50.0);
        for (double az = yaw - range; az <= yaw + range; az += step) {
            double n = 0.5 + 0.5 * Math.sin(az * 0.045 + time * 0.07) * Math.sin(az * 0.021 - time * 0.05 + 1.3);
            double e0 = 4 + 10 * Math.sin(az * 0.03 + time * 0.05);
            double hgt = 40 + 30 * n;
            double[] a = cam.project(az - step / 2, e0 + hgt * 0.5);
            double[] b = cam.project(az + step / 2, e0 + hgt * 0.5);
            double[] yt = cam.project(az, e0 + hgt);
            double[] yb = cam.project(az, e0);
            if (a == null || b == null || yt == null || yb == null) continue;
            double x0 = Math.min(a[0], b[0]), x1 = Math.max(a[0], b[0]);
            if (x1 - x0 < 1) x1 = x0 + 1;
            int slices = 7;
            for (int k = 0; k < slices; k++) {
                double f0 = k / (double) slices, f1 = (k + 1) / (double) slices, mid = (f0 + f1) / 2;
                double ya = yt[1] + (yb[1] - yt[1]) * f0, yb2 = yt[1] + (yb[1] - yt[1]) * f1;
                double a1 = 255 * 0.22 * intensity * n * Math.sin(Math.PI * mid);
                int c = mix(mix(top, bottom, 0.5 + 0.5 * Math.sin(az * 0.02 + time * 0.1)), 0x3DA3FF, mid * 0.4);
                box(g, cam.gw, cam.gh, x0, ya, x1, yb2 + 1, alpha(a1, c));
            }
        }
    }

    /** Thin white swirling lines on a black sky. */
    private static void drawSwirl(GuiGraphics g, Cam cam, double time, double intensity) {
        for (int k = 0; k < 9; k++) {
            double prevX = Double.NaN, prevY = Double.NaN;
            for (int j = 0; j <= 90; j++) {
                double s = j / 90.0;
                double az = k * 40 + 70 * Math.sin(s * 2.6 + k * 1.7 + time * 0.10);
                double el = 4 + 72 * s + 10 * Math.sin(s * 7 + k + time * 0.2);
                double[] p = cam.project(az, el);
                if (p == null) { prevX = Double.NaN; continue; }
                double edge = Math.sin(Math.PI * s);
                double a = 230 * intensity * Math.pow(edge, 0.6);
                double w = 1.0 + 1.4 * edge;
                box(g, cam.gw, cam.gh, p[0] - 1, p[1] - 1, p[0] + w + 1, p[1] + w + 1, alpha(a * 0.22, 0xFFFFFF));
                box(g, cam.gw, cam.gh, p[0], p[1], p[0] + w, p[1] + w, alpha(a, 0xFFFFFF));
                prevX = p[0];
                prevY = p[1];
            }
        }
    }

    private static void drawSunset(GuiGraphics g, Cam cam, int horizon, double intensity, double time) {
        int[] cols = { 0xFFB347, 0xFF7A45, 0xFF4F6D, 0xC04FA8, 0x7A4FD0, 0x3B3A9A };
        double prev = horizon;
        for (int i = 0; i < cols.length; i++) {
            double yy = horizonLine(cam, (i + 1) * 6.0);
            if (Double.isNaN(yy)) break;
            double wob = 4 * Math.sin(time * 0.3 + i);
            box(g, cam.gw, cam.gh, 0, yy + wob, cam.gw, prev + wob, alpha(255 * 0.30 * intensity * (1 - i / 7.0), cols[i]));
            prev = yy;
        }
    }

    /** Screen y of an elevation line straight ahead of the camera. */
    private static double horizonLine(Cam cam, double elDeg) {
        double yawAz = Math.toDegrees(Math.atan2(-cam.fx, cam.fz));
        double[] p = cam.project(yawAz, elDeg);
        return p == null ? Double.NaN : p[1];
    }
}
