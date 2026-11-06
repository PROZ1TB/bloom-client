package dev.bloom.risky.menu;

import dev.bloom.Config;
import dev.bloom.Theme;
import dev.bloom.screen.BloomScreen;
import dev.bloom.ui.Input;
import dev.bloom.ui.Ui;
import dev.bloom.visual.Avatar;
import dev.bloom.visual.SkyArt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

/** The Bloom main menu: animated sky, your character walks in and looks at your mouse. */
public class BloomTitleScreen extends Screen {
    /** Set when something went wrong; the vanilla title screen is used from then on. */
    public static boolean disabled;

    private static final String[] LABELS = { "Singleplayer", "Multiplayer", "Options", "Bloom Menu", "Quit Game" };

    private final Input.Edges ed = new Input.Edges();
    private final long opened = System.nanoTime();

    public BloomTitleScreen() {
        super(Component.literal("Bloom Client"));
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float pt) { }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        try {
            draw(g, mx, my);
        } catch (Throwable t) {
            disabled = true;
            System.out.println("[Bloom] main menu failed, using the vanilla one: " + t);
            t.printStackTrace();
            Minecraft.getInstance().setScreen(new TitleScreen());
        }
    }

    private static int mixRgb(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int gg = (int) Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (gg << 8) | bl;
    }

    private void draw(GuiGraphics g, int mx, int my) {
        ed.update();
        Ui.beginFrame();
        boolean click = ed.pressed(0);
        Minecraft mc = Minecraft.getInstance();
        double t = (System.nanoTime() - opened) / 1.0e9;
        int w = width, h = height;
        double pitch = -14, fov = 75;
        int hy = Math.max(h / 4, Math.min(h - h / 5, SkyArt.horizonY(h, pitch, fov)));
        int style = Math.floorMod((int) Config.getDouble("menu.sky", 0), SkyArt.STYLES.length);

        // sky base, then ground
        int skyTop = Theme.glass ? 0x2C5A82 : 0x05030C, skyLow = Theme.glass ? 0x8FC3E8 : mixRgb(0x10082A, Theme.accent, 0.28);
        int slices = 16;
        for (int i = 0; i < slices; i++) {
            int y0 = hy * i / slices, y1 = hy * (i + 1) / slices;
            g.fill(0, y0, w, y1, 0xFF000000 | mixRgb(skyTop, skyLow, (i + 0.5) / slices));
        }
        int gTop = Theme.glass ? 0x6E9CC0 : mixRgb(0x1A1D44, Theme.accent, 0.12), gBot = Theme.glass ? 0x2A4A6A : 0x04040A;
        for (int i = 0; i < slices; i++) {
            int y0 = hy + (h - hy) * i / slices, y1 = hy + (h - hy) * (i + 1) / slices;
            g.fill(0, y0, w, y1, 0xFF000000 | mixRgb(gTop, gBot, (i + 0.5) / slices));
        }
        SkyArt.draw(g, w, h, t * 2.5, pitch, fov, style, 1.0, t, true, true, true, Theme.accent, 1);

        // character walks in from the left, then looks at the mouse
        int u = Math.max(2, Math.round(h / 80f));
        int gy = hy + (int) ((h - hy) * 0.62);
        int target = (int) (w * 0.27), startX = -10 * u;
        double speed = w * 0.10;
        double cx = Math.min(target, startX + speed * t);
        boolean moving = cx < target - 0.5;
        double headX = cx, headY = gy - 28.0 * u;
        double lookX = moving ? 0.6 : Math.max(-1, Math.min(1, (mx - headX) / (w * 0.4)));
        double lookY = moving ? 0 : Math.max(-1, Math.min(1, (my - headY) / (h * 0.35)));
        Ui.rrect(g, (int) cx - 6 * u, gy - u / 2, 12 * u, u, u / 2, Theme.argb(0x55, 0x000000));
        Avatar.paint(g, (int) cx, gy, u, t * 8.0, moving, lookX, lookY, t);

        // logo
        int bx = (int) (w * 0.68) - 70;
        var m = g.pose();
        m.pushMatrix();
        m.translate((float) (bx - 6), (float) (h * 0.16));
        m.scale(3f, 3f);
        try {
            String logo = "Bloom Client";
            int a = Theme.glass ? 0xFFFFFFFF : Theme.ACCENT;
            Ui.textFlat(g, logo, 1, 0, Theme.withAlpha(a, 0x40));
            Ui.textFlat(g, logo, -1, 0, Theme.withAlpha(a, 0x40));
            Ui.textFlat(g, logo, 0, 1, Theme.withAlpha(a, 0x40));
            Ui.text(g, logo, 0, 0, 0xFFFFFFFF);
        } finally {
            m.popMatrix();
        }
        Ui.textFlat(g, "Your client. Your style.", bx - 4, (int) (h * 0.16) + 30, Theme.MUTED);

        // buttons
        int by = (int) (h * 0.34), bw = 140, bh = 20, gap = 6;
        Ui.panel(g, bx - 10, by - 10, bw + 20, LABELS.length * (bh + gap) + 8, Theme.radius + 2);
        for (int i = 0; i < LABELS.length; i++) {
            int y = by + i * (bh + gap);
            boolean hov = Ui.in(mx, my, bx, y, bw, bh);
            Ui.button(g, bx, y, bw, bh, LABELS[i], hov);
            if (click && hov) {
                act(mc, i);
                return;
            }
        }
        Ui.textFlat(g, "Bloom Client", 6, h - 12, Theme.withAlpha(Theme.MUTED, 0x90));
    }

    private void act(Minecraft mc, int i) {
        switch (i) {
            case 0: mc.setScreen(new SelectWorldScreen(this)); break;
            case 1: mc.setScreen(new JoinMultiplayerScreen(this)); break;
            case 2: mc.setScreen(new OptionsScreen(this, mc.options)); break;
            case 3: mc.setScreen(new BloomScreen(this)); break;
            default: mc.stop(); break;
        }
    }
}
