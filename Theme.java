package dev.bloom;

import java.util.ArrayList;
import java.util.List;

/** Colors for the whole Bloom GUI (menu and HUD). Everything reads from here. */
public final class Theme {
    public static int accent = 0xA17BFF;      // RGB
    public static boolean glass = false;      // the "Glass" color: clear and bubbly
    public static int radius = 6;             // corner radius in GUI pixels
    public static final List<Integer> favs = new ArrayList<>();

    public static final int[] PALETTE = {
        0xA17BFF, 0x7C4DFF, 0xB84DFF, 0xFF5CC8, 0xFF5555, 0xFF8A3D, 0xFFC93D, 0xC8E63D,
        0x6BE675, 0x2ED8A3, 0x3DD6E6, 0x4DA3FF, 0x4D6BFF, 0xFFFFFF, 0xBFC7D5, 0x2B2B38
    };

    public static int PANEL, CARD, CARD_HOVER, FIELD, BTN, BTN_HOVER, BORDER, ACCENT, ACCENT_DIM;
    public static int TITLE = 0xFFFFFFFF, TEXT = 0xFFFFFFFF, MUTED, GOOD = 0xFF6BE675, BAD = 0xFFFF6B6B;

    static { apply(); }

    public static void setAccent(int rgb) {
        accent = rgb & 0xFFFFFF;
        glass = false;
        apply();
        Config.put("theme.accent", Integer.toHexString(accent));
        Config.put("theme.glass", "false");
    }

    public static void setGlass(boolean g) {
        glass = g;
        apply();
        Config.put("theme.glass", Boolean.toString(g));
    }

    public static void setRadius(int r) {
        radius = Math.max(2, Math.min(12, r));
        Config.put("theme.radius", Integer.toString(radius));
    }

    public static void load() {
        try {
            accent = Integer.parseInt(Config.get("theme.accent", Integer.toHexString(accent)), 16) & 0xFFFFFF;
        } catch (NumberFormatException ignored) { }
        glass = Boolean.parseBoolean(Config.get("theme.glass", "false"));
        try {
            radius = Math.max(2, Math.min(12, Integer.parseInt(Config.get("theme.radius", "6"))));
        } catch (NumberFormatException ignored) { }
        favs.clear();
        String f = Config.get("theme.favs", "");
        for (String s : f.split(",")) {
            s = s.trim();
            if (s.isEmpty()) continue;
            try { favs.add(Integer.parseInt(s, 16) & 0xFFFFFF); } catch (NumberFormatException ignored) { }
        }
        apply();
    }

    public static void saveFavs() {
        StringBuilder sb = new StringBuilder();
        for (int c : favs) {
            if (sb.length() > 0) sb.append(',');
            sb.append(Integer.toHexString(c));
        }
        Config.put("theme.favs", sb.toString());
    }

    public static boolean addFav(int rgb) {
        rgb &= 0xFFFFFF;
        if (favs.contains(rgb) || favs.size() >= 14) return false;
        favs.add(rgb);
        saveFavs();
        return true;
    }

    public static void removeFav(int rgb) {
        favs.remove(Integer.valueOf(rgb & 0xFFFFFF));
        saveFavs();
    }

    public static void apply() {
        if (glass) {
            PANEL = argb(0x3A, 0xDDF2FF);
            CARD = argb(0x30, 0xFFFFFF);
            CARD_HOVER = argb(0x52, 0xFFFFFF);
            FIELD = argb(0x28, 0xFFFFFF);
            BTN = argb(0x48, 0xFFFFFF);
            BTN_HOVER = argb(0x72, 0xFFFFFF);
            BORDER = argb(0xD0, 0xFFFFFF);
            ACCENT = 0xFFBFEAFF;
            ACCENT_DIM = argb(0x90, 0xBFEAFF);
            MUTED = 0xFFE3F4FF;
            return;
        }
        float[] hsv = rgbToHsv(accent);
        float h = hsv[0], s = hsv[1];
        PANEL = argb(0xC8, hsvToRgb(h, Math.min(1f, s * 1.15f), 0.16f));
        CARD = argb(0xB0, hsvToRgb(h, Math.min(1f, s * 1.05f), 0.27f));
        CARD_HOVER = argb(0xD8, hsvToRgb(h, Math.min(1f, s * 0.95f), 0.38f));
        FIELD = argb(0xCC, hsvToRgb(h, Math.min(1f, s * 1.2f), 0.12f));
        BTN = argb(0xCC, hsvToRgb(h, s, Math.max(0.4f, Math.min(0.72f, hsv[2] * 0.7f))));
        BTN_HOVER = argb(0xFF, hsvToRgb(h, Math.min(1f, s * 0.9f), Math.max(0.55f, Math.min(0.88f, hsv[2] * 0.86f))));
        BORDER = argb(0xE8, accent);
        ACCENT = 0xFF000000 | accent;
        ACCENT_DIM = argb(0xFF, hsvToRgb(h, s * 0.8f, 0.55f));
        MUTED = 0xFF000000 | hsvToRgb(h, s * 0.35f, 0.88f);
    }

    public static int argb(int a, int rgb) {
        return ((a & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    public static int withAlpha(int argb, int a) {
        return ((a & 0xFF) << 24) | (argb & 0xFFFFFF);
    }

    public static float[] rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f, g = ((rgb >> 8) & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float d = max - min, h;
        if (d == 0f) h = 0f;
        else if (max == r) h = ((g - b) / d) % 6f;
        else if (max == g) h = (b - r) / d + 2f;
        else h = (r - g) / d + 4f;
        h /= 6f;
        if (h < 0f) h += 1f;
        return new float[] { h, max == 0f ? 0f : d / max, max };
    }

    public static int hsvToRgb(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        float c = v * s, x = c * (1f - Math.abs((h * 6f) % 2f - 1f)), m = v - c;
        float r, g, b;
        int seg = (int) (h * 6f) % 6;
        switch (seg) {
            case 0: r = c; g = x; b = 0; break;
            case 1: r = x; g = c; b = 0; break;
            case 2: r = 0; g = c; b = x; break;
            case 3: r = 0; g = x; b = c; break;
            case 4: r = x; g = 0; b = c; break;
            default: r = c; g = 0; b = x; break;
        }
        int ri = Math.round((r + m) * 255f), gi = Math.round((g + m) * 255f), bi = Math.round((b + m) * 255f);
        return (ri << 16) | (gi << 8) | bi;
    }
}
