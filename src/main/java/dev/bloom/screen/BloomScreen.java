package dev.bloom.screen;

import dev.bloom.Config;
import dev.bloom.Theme;
import dev.bloom.lyrics.LyricsEngine;
import dev.bloom.module.Module;
import dev.bloom.module.ModuleManager;
import dev.bloom.module.Setting;
import dev.bloom.ui.Input;
import dev.bloom.ui.Ui;
import dev.bloom.visual.SkyArt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** The Bloom menu. Drawn entirely with the Bloom theme so the Theme tab restyles all of it. */
public class BloomScreen extends Screen {
    private static final int VW = 470, VH = 290;
    private static final String[] TABS = { "Modules", "Theme", "Lyrics" };
    private static final String[] CATS = { "All", "HUD", "Visual", "Utility", "Music" };

    private int tab;
    private String cat = "All";
    private int scroll, maxScroll;
    private String open;
    private Setting.Num dragNum;
    private int dragX, dragW;
    private boolean dragRadius;
    private boolean hexFocus;
    private String hex = "";
    private List<String> songs = new ArrayList<>();
    private final Input.Edges ed = new Input.Edges();
    private final long openedAt = System.nanoTime();

    private final Screen parent;

    public BloomScreen() { this(null); }

    public BloomScreen(Screen parent) {
        super(Component.literal("Bloom Client"));
        this.parent = parent;
        songs = LyricsEngine.list();
    }

    @Override
    public void onClose() {
        Config.save();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float pt) { }

    @Override
    public void removed() { Config.save(); }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.round(sy * 16)));
        return true;
    }

    // ------------------------------------------------------------------ frame

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        ed.update();
        boolean click = ed.pressed(0), rclick = ed.pressed(1), down = ed.down(0);

        // The menu floats in the middle of the game at about 60% of the screen and grows in when opened.
        float age = Math.min(1f, (System.nanoTime() - openedAt) / 280_000_000f);
        float ease = 1f - (1f - age) * (1f - age) * (1f - age);
        float fs = Math.max(0.5f, Math.min(1f, Math.min(width * 0.62f / VW, height * 0.68f / VH)));
        float sc = fs * (0.8f + 0.2f * ease);
        double t = System.nanoTime() / 1.0e9;
        float bob = (float) Math.sin(t * 1.4) * 0.8f * fs;
        float ox = (width - VW * sc) / 2f - (mx - width / 2f) * 0.012f, oy = (height - VH * sc) / 2f + bob - (my - height / 2f) * 0.012f;
        int vmx = Math.round((mx - ox) / sc), vmy = Math.round((my - oy) / sc);

        var m = g.pose();
        m.pushMatrix();
        m.translate(ox, oy);
        m.scale(sc, sc);
        try {
            drawMenu(g, vmx, vmy, click, rclick, down);
        } finally {
            m.popMatrix();
        }
    }

    private void drawMenu(GuiGraphics g, int mx, int my, boolean click, boolean rclick, boolean down) {
        int pw = VW, ph = VH, px = 0, py = 0;

        // soft glow around the panel (hollow rings, so nothing overlaps the panel itself)
        int glowRgb = Theme.glass ? 0xBFEAFF : Theme.accent;
        for (int k = 1; k <= 5; k++) {
            Ui.ring(g, px - k, py - k, pw + 2 * k, ph + 2 * k, Theme.radius + 3 + k, Theme.argb(0x30 - k * 6, glowRgb), 1);
        }
        Ui.panel(g, px, py, pw, ph, Theme.radius + 3);
        if (Theme.glass) Ui.bubbles(g, px, py, pw, ph);
        Ui.text(g, "Bloom", px + 12, py + 9, Theme.ACCENT);
        Ui.text(g, "Client", px + 12 + Ui.width("Bloom "), py + 9, Theme.TITLE);

        for (int i = 0; i < TABS.length; i++) {
            int tx = px + 8, ty = py + 30 + i * 22;
            boolean hov = Ui.in(mx, my, tx, ty, 74, 18);
            Ui.rrect(g, tx, ty, 74, 18, Math.max(2, Theme.radius - 1), tab == i ? Theme.BTN_HOVER : hov ? Theme.CARD_HOVER : Theme.CARD);
            Ui.centered(g, TABS[i], tx + 37, ty + 5, Theme.TEXT);
            if (click && hov && tab != i) {
                tab = i;
                scroll = 0;
                open = null;
                hexFocus = false;
                if (i == 2) songs = LyricsEngine.list();
            }
        }

        if (!down) { dragNum = null; dragRadius = false; }

        int cx = px + 90, cy = py + 8, cw = pw - 98, ch = ph - 16;
        switch (tab) {
            case 0: drawModules(g, mx, my, click, down, cx, cy, cw, ch); break;
            case 1: drawTheme(g, mx, my, click, rclick, down, cx, cy, cw, ch); break;
            default: drawLyrics(g, mx, my, click, cx, cy, cw, ch); break;
        }
    }

    // ------------------------------------------------------------------ modules tab

    private void drawModules(GuiGraphics g, int mx, int my, boolean click, boolean down, int cx, int cy, int cw, int ch) {
        int x = cx;
        for (String c : CATS) {
            int w = Ui.width(c) + 14;
            boolean hov = Ui.in(mx, my, x, cy, w, 16);
            Ui.rrect(g, x, cy, w, 16, 7, c.equals(cat) ? Theme.BTN_HOVER : hov ? Theme.CARD_HOVER : Theme.CARD);
            Ui.text(g, c, x + 7, cy + 4, Theme.TEXT);
            if (click && hov) { cat = c; scroll = 0; open = null; }
            x += w + 4;
        }
        boolean eh = Ui.in(mx, my, cx + cw - 80, cy, 80, 16);
        Ui.button(g, cx + cw - 80, cy, 80, 16, "HUD Editor", eh);
        if (click && eh) {
            Minecraft.getInstance().setScreen(new HudEditorScreen(this));
            return;
        }

        int listY = cy + 22, listH = ch - 22 - 12;
        boolean inList = Ui.in(mx, my, cx, listY, cw, listH);
        List<Module> mods = new ArrayList<>();
        for (Module m : ModuleManager.all()) if (cat.equals("All") || m.category.equals(cat)) mods.add(m);

        int total = 0;
        for (Module m : mods) total += 26 + (m.id.equals(open) ? m.settings.size() * 18 + 6 : 0);
        maxScroll = Math.max(0, total - listH);
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        g.enableScissor(cx, listY, cx + cw, listY + listH);
        try {
            int y = listY - scroll;
            for (Module m : mods) {
                boolean exp = m.id.equals(open);
                int setH = exp ? m.settings.size() * 18 + 6 : 0;
                int rowW = cw - 6;
                if (y + 24 + setH >= listY && y <= listY + listH) {
                    boolean hov = inList && Ui.in(mx, my, cx, y, rowW, 24);
                    Ui.card(g, cx, y, rowW, 24 + setH, Theme.radius, hov);
                    Ui.text(g, m.name, cx + 8, y + 4, Theme.TEXT);
                    Ui.textFlat(g, Ui.fit(m.description, rowW - 80), cx + 8, y + 14, Theme.MUTED);
                    int tgx = cx + rowW - 30, tgy = y + 6;
                    Ui.toggle(g, tgx, tgy, m.enabled);
                    if (click && hov) {
                        if (Ui.in(mx, my, tgx - 4, tgy - 3, 30, 17)) m.toggle();
                        else if (!m.settings.isEmpty()) open = exp ? null : m.id;
                    }
                    if (exp) drawSettings(g, m, mx, my, click, down, inList, cx, y + 26, rowW);
                }
                y += 26 + setH;
            }
        } finally {
            g.disableScissor();
        }
        if (dragNum != null && down) dragNum.setFraction((mx - dragX) / (double) dragW);
    }

    private void drawSettings(GuiGraphics g, Module m, int mx, int my, boolean click, boolean down,
                              boolean inList, int cx, int y, int rowW) {
        int rx = cx + rowW - 8;
        for (Setting s : m.settings) {
            Ui.text(g, s.name, cx + 14, y + 1, Theme.TEXT);
            if (s instanceof Setting.Bool b) {
                int tx = rx - 22;
                Ui.toggle(g, tx, y, b.value);
                if (click && inList && Ui.in(mx, my, tx - 4, y - 3, 30, 17)) b.toggle();
            } else if (s instanceof Setting.Num n) {
                int tw = 84, tx = rx - tw - 34;
                Ui.slider(g, tx, y, tw, n.fraction());
                Ui.textFlat(g, n.display(), tx + tw + 8, y + 1, Theme.MUTED);
                if (click && inList && Ui.in(mx, my, tx - 3, y - 2, tw + 6, 13)) {
                    dragNum = n;
                    dragX = tx;
                    dragW = tw;
                    n.setFraction((mx - tx) / (double) tw);
                }
            } else if (s instanceof Setting.Mode md) {
                int bw = 70, bx = rx - bw;
                boolean hov = inList && Ui.in(mx, my, bx, y - 2, bw, 13);
                Ui.button(g, bx, y - 2, bw, 13, Ui.fit(md.get(), bw - 6), hov);
                if (click && hov) md.cycle();
            } else if (s instanceof Setting.Color c) {
                int bx = rx - 20;
                boolean hov = inList && Ui.in(mx, my, bx - 2, y - 2, 24, 13);
                Ui.rrect(g, bx, y, 20, 9, 3, c.argb());
                if (hov) Ui.ring(g, bx - 1, y - 1, 22, 11, 4, 0xFFFFFFFF, 1);
                if (click && hov) c.cycle();
            }
            y += 18;
        }
    }

    // ------------------------------------------------------------------ theme tab

    private void drawTheme(GuiGraphics g, int mx, int my, boolean click, boolean rclick, boolean down,
                           int cx, int cy, int cw, int ch) {
        Ui.text(g, "Accent color", cx, cy, Theme.TITLE);
        int sw = 18, gap = 5, perRow = Math.max(1, cw / (sw + gap));
        int total = Theme.PALETTE.length + 1;
        int gy = cy + 14;
        for (int i = 0; i < total; i++) {
            int x = cx + (i % perRow) * (sw + gap), y = gy + (i / perRow) * (sw + gap);
            boolean hov = Ui.in(mx, my, x, y, sw, sw);
            boolean isGlass = i == 0;
            boolean sel;
            if (isGlass) {
                Ui.rrect(g, x, y, sw, sw, 9, Theme.argb(0x50, 0xFFFFFF), Theme.argb(0xE0, 0xFFFFFF), 1);
                Ui.rrect(g, x + 4, y + 3, 4, 4, 2, Theme.argb(0xC0, 0xFFFFFF));
                Ui.rrect(g, x + 10, y + 9, 5, 5, 2, Theme.argb(0x90, 0xBFEAFF));
                sel = Theme.glass;
            } else {
                int rgb = Theme.PALETTE[i - 1];
                Ui.rrect(g, x, y, sw, sw, 5, 0xFF000000 | rgb);
                sel = !Theme.glass && Theme.accent == rgb;
            }
            if (sel) Ui.ring(g, x - 2, y - 2, sw + 4, sw + 4, isGlass ? 11 : 7, 0xFFFFFFFF, 2);
            else if (hov) Ui.ring(g, x - 1, y - 1, sw + 2, sw + 2, isGlass ? 10 : 6, Theme.argb(0xA0, 0xFFFFFF), 1);
            if (click && hov) {
                if (isGlass) Theme.setGlass(true); else Theme.setAccent(Theme.PALETTE[i - 1]);
            }
        }
        int rows = (total + perRow - 1) / perRow;
        int y = gy + rows * (sw + gap) + 2;
        Ui.textFlat(g, Theme.glass ? "Glass: everything turns clear and bubbly." : "Pick Glass (first swatch) for a clear, bubbly look.", cx, y, Theme.MUTED);
        y += 14;

        // hex entry
        Ui.text(g, "Hex", cx, y + 3, Theme.TEXT);
        int hx = cx + 26;
        boolean hh = Ui.in(mx, my, hx, y, 66, 14);
        Ui.rrect(g, hx, y, 66, 14, 3, Theme.FIELD, hexFocus ? Theme.ACCENT : Theme.argb(0x70, 0xFFFFFF), 1);
        String shown = hexFocus ? hex + ((System.currentTimeMillis() / 450) % 2 == 0 ? "_" : "") : String.format("%06X", Theme.accent);
        Ui.text(g, "#" + shown, hx + 4, y + 3, Theme.TEXT);
        if (click) hexFocus = hh;
        if (click && hh) hex = "";
        if (hexFocus) typeHex();
        int sx = hx + 72;
        boolean sh = Ui.in(mx, my, sx, y - 1, 64, 16);
        Ui.button(g, sx, y - 1, 64, 16, "Save fav", sh);
        if (click && sh) Theme.addFav(Theme.accent);
        y += 24;

        // favorites
        Ui.text(g, "Favorites", cx, y, Theme.TITLE);
        Ui.textFlat(g, "click = use, right click = remove", cx + 58, y + 1, Theme.MUTED);
        y += 14;
        if (Theme.favs.isEmpty()) Ui.textFlat(g, "None yet. Pick a color, then press Save fav.", cx, y + 3, Theme.MUTED);
        Integer remove = null, apply = null;
        for (int i = 0; i < Theme.favs.size(); i++) {
            int fcol = Theme.favs.get(i);
            int x = cx + i * 20;
            boolean hov = Ui.in(mx, my, x, y, 16, 16);
            Ui.rrect(g, x, y, 16, 16, 5, 0xFF000000 | fcol);
            if (hov) Ui.ring(g, x - 1, y - 1, 18, 18, 6, 0xFFFFFFFF, 1);
            if (hov && click) apply = fcol;
            if (hov && rclick) remove = fcol;
        }
        if (apply != null) Theme.setAccent(apply);
        if (remove != null) Theme.removeFav(remove);
        y += 26;

        // corner roundness
        Ui.text(g, "Roundness", cx, y, Theme.TEXT);
        int tx = cx + 64, tw = 100;
        double frac = (Theme.radius - 2) / 10.0;
        Ui.slider(g, tx, y, tw, frac);
        Ui.textFlat(g, Integer.toString(Theme.radius), tx + tw + 8, y + 1, Theme.MUTED);
        if (click && Ui.in(mx, my, tx - 3, y - 2, tw + 6, 13)) dragRadius = true;
        if (dragRadius && down) Theme.setRadius(2 + (int) Math.round(Math.max(0, Math.min(1, (mx - tx) / (double) tw)) * 10));

        y += 24;
        Ui.text(g, "Custom main menu", cx, y + 1, Theme.TEXT);
        boolean menuOn = Config.getBool("menu.enabled", true);
        Ui.toggle(g, cx + 110, y, menuOn);
        if (click && Ui.in(mx, my, cx + 106, y - 3, 30, 17)) Config.put("menu.enabled", Boolean.toString(!menuOn));
        y += 22;
        Ui.text(g, "Menu sky", cx, y + 3, Theme.TEXT);
        int sk = Math.floorMod((int) Config.getDouble("menu.sky", 0), SkyArt.STYLES.length);
        boolean skh = Ui.in(mx, my, cx + 110, y, 80, 16);
        Ui.button(g, cx + 110, y, 80, 16, SkyArt.STYLES[sk], skh);
        if (click && skh) Config.put("menu.sky", Integer.toString((sk + 1) % SkyArt.STYLES.length));
    }

    private void typeHex() {
        for (int k = GLFW.GLFW_KEY_0; k <= GLFW.GLFW_KEY_9; k++)
            if (ed.keyPressed(k) && hex.length() < 6) hex += (char) k;
        for (int k = GLFW.GLFW_KEY_A; k <= GLFW.GLFW_KEY_F; k++)
            if (ed.keyPressed(k) && hex.length() < 6) hex += (char) k;
        if (ed.keyPressed(GLFW.GLFW_KEY_BACKSPACE) && !hex.isEmpty()) hex = hex.substring(0, hex.length() - 1);
        if (hex.length() == 6 || (ed.keyPressed(GLFW.GLFW_KEY_ENTER) && !hex.isEmpty())) {
            try {
                Theme.setAccent(Integer.parseInt(hex, 16));
            } catch (NumberFormatException ignored) { }
            hexFocus = false;
        }
    }

    // ------------------------------------------------------------------ lyrics tab

    private void drawLyrics(GuiGraphics g, int mx, int my, boolean click, int cx, int cy, int cw, int ch) {
        Ui.text(g, "Lyrics in the Air", cx, cy, Theme.TITLE);
        Ui.textFlat(g, "Add song.lrc (timed lyrics) and optional song.wav to:", cx, cy + 13, Theme.MUTED);
        Ui.textFlat(g, Ui.fit(LyricsEngine.dir().toString(), cw), cx, cy + 23, Theme.MUTED);

        int y = cy + 38;
        Module lm = null;
        for (Module m : ModuleManager.all()) if (m.id.equals("lyrics")) lm = m;
        if (lm != null) {
            Ui.text(g, "Show lyrics on screen", cx, y + 1, Theme.TEXT);
            Ui.toggle(g, cx + 130, y, lm.enabled);
            if (click && Ui.in(mx, my, cx + 126, y - 3, 30, 17)) lm.toggle();
        }
        y += 18;

        int listH = Math.max(30, ch - 38 - 18 - 58);
        Ui.rrect(g, cx, y, cw - 6, listH, 4, Theme.FIELD);
        g.enableScissor(cx, y, cx + cw - 6, y + listH);
        try {
            if (songs.isEmpty()) Ui.textFlat(g, "No songs found. Drop an .lrc file in the folder above.", cx + 6, y + 6, Theme.MUTED);
            int ry = y + 2;
            for (String s : songs) {
                boolean sel = s.equals(LyricsEngine.song);
                boolean hov = Ui.in(mx, my, cx, ry, cw - 6, 14) && Ui.in(mx, my, cx, y, cw - 6, listH);
                if (sel || hov) Ui.rrect(g, cx + 2, ry, cw - 10, 14, 3, sel ? Theme.BTN_HOVER : Theme.CARD_HOVER);
                Ui.text(g, Ui.fit(s, cw - 24), cx + 6, ry + 3, Theme.TEXT);
                if (click && hov) LyricsEngine.select(s);
                ry += 15;
                if (ry > y + listH) break;
            }
        } finally {
            g.disableScissor();
        }
        y += listH + 6;

        boolean has = LyricsEngine.song != null;
        int bx = cx;
        String playLabel = LyricsEngine.isPlaying() ? "Pause" : "Play";
        boolean b1 = has && Ui.in(mx, my, bx, y, 50, 16);
        Ui.button(g, bx, y, 50, 16, playLabel, b1);
        if (click && b1) { if (LyricsEngine.isPlaying()) LyricsEngine.pause(); else LyricsEngine.play(); }
        bx += 54;
        boolean b2 = has && Ui.in(mx, my, bx, y, 56, 16);
        Ui.button(g, bx, y, 56, 16, "Restart", b2);
        if (click && b2) LyricsEngine.restart();
        bx += 64;
        boolean b3 = has && Ui.in(mx, my, bx, y, 22, 16);
        Ui.button(g, bx, y, 22, 16, "-", b3);
        if (click && b3) LyricsEngine.nudge(-250);
        Ui.centered(g, String.format("%+.2fs", LyricsEngine.offsetMs / 1000.0), bx + 22 + 26, y + 4, Theme.TEXT);
        bx += 22 + 52;
        boolean b4 = has && Ui.in(mx, my, bx, y, 22, 16);
        Ui.button(g, bx, y, 22, 16, "+", b4);
        if (click && b4) LyricsEngine.nudge(250);

        y += 22;
        Ui.textFlat(g, Ui.fit(LyricsEngine.status, cw - 8), cx, y, Theme.MUTED);
        Ui.textFlat(g, "Spotify: not connected yet (coming later).", cx, y + 11, Theme.MUTED);
    }
}
