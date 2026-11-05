package dev.bloom.module;

import dev.bloom.HudRenderer;
import dev.bloom.Theme;
import dev.bloom.ui.Input;
import dev.bloom.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** All the built-in HUD and utility modules. */
public final class Modules {
    private Modules() { }

    private static final DateTimeFormatter CLOCK24 = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter CLOCK12 = DateTimeFormatter.ofPattern("hh:mm:ss a");

    private static List<String> one(String s) {
        List<String> l = new ArrayList<>(1);
        l.add(s);
        return l;
    }

    private static String fmtTime(long totalSeconds) {
        long h = totalSeconds / 3600, m = (totalSeconds / 60) % 60, s = totalSeconds % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%d:%02d", m, s);
    }

    // ------------------------------------------------------------------ HUD

    public static final class Watermark extends TextHud {
        public Watermark() { super("watermark", "Watermark", "Shows the client name.", true, 0.005, 0.01); }
        @Override protected List<String> lines(Minecraft mc) { return one("Bloom Client"); }
    }

    public static final class Fps extends TextHud {
        public Fps() { super("fps", "FPS", "Frames per second.", true, 0.005, 0.07); }
        @Override protected List<String> lines(Minecraft mc) { return one("FPS: " + HudRenderer.fps); }
    }

    public static final class Cps extends TextHud {
        public Cps() { super("cps", "CPS", "Clicks per second (left | right).", true, 0.005, 0.13); }
        @Override protected List<String> lines(Minecraft mc) {
            return one("CPS: " + Input.cps(0) + " | " + Input.cps(1));
        }
    }

    public static final class Coordinates extends TextHud {
        private final Setting.Bool decimals = add(new Setting.Bool("decimals", "Decimals", false));
        public Coordinates() { super("coords", "Coordinates", "Your XYZ position.", true, 0.005, 0.19); }
        @Override protected List<String> lines(Minecraft mc) {
            if (mc.player == null) return one("XYZ: -");
            if (decimals.value) {
                return one(String.format("XYZ: %.1f %.1f %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ()));
            }
            return one("XYZ: " + (int) Math.floor(mc.player.getX()) + " " + (int) Math.floor(mc.player.getY())
                    + " " + (int) Math.floor(mc.player.getZ()));
        }
    }

    public static final class Direction extends TextHud {
        private static final String[] DIRS = { "South", "South-West", "West", "North-West", "North", "North-East", "East", "South-East" };
        public Direction() { super("direction", "Direction", "Which way you are facing.", false, 0.005, 0.25); }
        @Override protected List<String> lines(Minecraft mc) {
            if (mc.player == null) return one("Facing: -");
            float yaw = ((mc.player.getYRot() % 360f) + 360f) % 360f;
            int i = (int) Math.floor((yaw + 22.5f) / 45f) % 8;
            return one("Facing: " + DIRS[i]);
        }
    }

    public static final class Clock extends TextHud {
        private final Setting.Bool hour24 = add(new Setting.Bool("h24", "24 Hour", true));
        public Clock() { super("clock", "Clock", "Your computer's time.", false, 0.9, 0.01); }
        @Override protected List<String> lines(Minecraft mc) {
            return one(LocalTime.now().format(hour24.value ? CLOCK24 : CLOCK12));
        }
    }

    public static final class Playtime extends TextHud {
        private long ticks;
        public Playtime() { super("playtime", "Playtime", "Time spent in this world.", false, 0.9, 0.07); }
        @Override public void tick(Minecraft mc) {
            if (mc.level != null) ticks++; else ticks = 0;
        }
        @Override protected List<String> lines(Minecraft mc) { return one("Time: " + fmtTime(ticks / 20)); }
    }

    public static final class Memory extends TextHud {
        public Memory() { super("memory", "Memory", "Game memory use.", false, 0.9, 0.13); }
        @Override protected List<String> lines(Minecraft mc) {
            Runtime r = Runtime.getRuntime();
            long used = (r.totalMemory() - r.freeMemory()) / 1048576L, max = r.maxMemory() / 1048576L;
            return one("Mem: " + used + "/" + max + " MB");
        }
    }

    public static final class Speed extends TextHud {
        private double lastX, lastZ, speed;
        private boolean have;
        public Speed() { super("speed", "Speed", "Blocks per second.", false, 0.9, 0.19); }
        @Override public void tick(Minecraft mc) {
            if (mc.player == null) { have = false; return; }
            double x = mc.player.getX(), z = mc.player.getZ();
            if (have) {
                double v = Math.sqrt((x - lastX) * (x - lastX) + (z - lastZ) * (z - lastZ)) * 20.0;
                speed += (v - speed) * 0.3;
            }
            lastX = x;
            lastZ = z;
            have = true;
        }
        @Override protected List<String> lines(Minecraft mc) { return one(String.format("Speed: %.1f b/s", speed)); }
    }

    public static final class Ping extends TextHud {
        public Ping() { super("ping", "Ping", "Your latency to the server.", false, 0.9, 0.25); }
        @Override protected List<String> lines(Minecraft mc) {
            if (mc.player == null) return one("Ping: -");
            var conn = mc.getConnection();
            if (conn == null) return one("Ping: -");
            var info = conn.getPlayerInfo(mc.player.getUUID());
            return one(info == null ? "Ping: -" : "Ping: " + info.getLatency() + " ms");
        }
    }

    public static final class ServerAddress extends TextHud {
        public ServerAddress() { super("server", "Server", "The server you are on.", false, 0.005, 0.31); }
        @Override protected List<String> lines(Minecraft mc) {
            var sd = mc.getCurrentServer();
            return one(sd == null ? "Singleplayer" : sd.ip);
        }
    }

    public static final class HeldItem extends TextHud {
        public HeldItem() { super("helditem", "Held Item", "Name and durability of your item.", false, 0.45, 0.9); }
        @Override protected List<String> lines(Minecraft mc) {
            if (mc.player == null) return new ArrayList<>();
            ItemStack st = mc.player.getMainHandItem();
            if (st.isEmpty()) return new ArrayList<>();
            String n = st.getHoverName().getString();
            if (st.isDamageableItem()) n += " (" + (st.getMaxDamage() - st.getDamageValue()) + "/" + st.getMaxDamage() + ")";
            return one(n);
        }
    }

    public static final class PotionStatus extends TextHud {
        private static final String[] ROMAN = { "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X" };
        public PotionStatus() { super("potions", "Potion Status", "Active effects and time left.", true, 0.005, 0.42); }
        @Override protected List<String> lines(Minecraft mc) {
            List<String> out = new ArrayList<>();
            if (mc.player == null) return out;
            for (var e : mc.player.getActiveEffects()) {
                String name = Component.translatable(e.getDescriptionId()).getString();
                int amp = e.getAmplifier();
                String lvl = amp >= 0 && amp < ROMAN.length ? " " + ROMAN[amp] : " " + (amp + 1);
                String time = e.getDuration() < 0 ? "inf" : fmtTime(e.getDuration() / 20);
                out.add(name + lvl + "  " + time);
            }
            return out;
        }
    }

    public static final class ArmorStatus extends HudModule {
        private static final EquipmentSlot[] SLOTS = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
        private final Setting.Mode show = add(new Setting.Mode("show", "Show", 0, "Durability", "Percent", "Icons only"));

        public ArmorStatus() { super("armor", "Armor Status", "HUD", "Your armor and how worn it is.", true, 0.9, 0.5); }

        private String label(ItemStack st) {
            if (!st.isDamageableItem()) return "";
            int left = st.getMaxDamage() - st.getDamageValue();
            if (show.index == 1) return Math.round(left * 100f / st.getMaxDamage()) + "%";
            if (show.index == 2) return "";
            return Integer.toString(left);
        }

        @Override protected int[] measure(Minecraft mc) {
            if (mc.player == null) return new int[] { 0, 0 };
            int rows = 0, w = 20;
            for (EquipmentSlot s : SLOTS) {
                ItemStack st = mc.player.getItemBySlot(s);
                if (st.isEmpty()) continue;
                rows++;
                w = Math.max(w, 22 + Ui.width(label(st)) + 4);
            }
            return rows == 0 ? new int[] { 0, 0 } : new int[] { w, rows * 18 + 2 };
        }

        @Override protected void draw(GuiGraphics g, Minecraft mc, int w, int h) {
            int y = 1;
            for (EquipmentSlot s : SLOTS) {
                ItemStack st = mc.player.getItemBySlot(s);
                if (st.isEmpty()) continue;
                g.renderItem(st, 3, y);
                String l = label(st);
                if (!l.isEmpty()) {
                    int col = st.isDamageableItem() && st.getDamageValue() * 4 > st.getMaxDamage() * 3 ? Theme.BAD : textColor();
                    Ui.text(g, l, 22, y + 4, col);
                }
                y += 18;
            }
        }
    }

    public static final class Keystrokes extends HudModule {
        private final Setting.Bool space = add(new Setting.Bool("space", "Show Space", true));
        private final Setting.Bool mouse = add(new Setting.Bool("mouse", "Show Mouse", true));

        public Keystrokes() { super("keystrokes", "Keystrokes", "HUD", "WASD, space and mouse buttons.", true, 0.005, 0.7); }

        @Override protected int[] measure(Minecraft mc) {
            int h = 4 + 18 + 2 + 18;
            if (space.value) h += 2 + 12;
            if (mouse.value) h += 2 + 16;
            return new int[] { 4 + 58, h + 2 };
        }

        private void key(GuiGraphics g, int x, int y, int w, int h, String label, boolean down) {
            int fill = down ? Theme.withAlpha(Theme.ACCENT, 0xD0) : Theme.argb(Theme.glass ? 0x30 : 0x70, 0x000000);
            Ui.rrect(g, x, y, w, h, 3, fill);
            if (Theme.glass) Ui.ring(g, x, y, w, h, 3, Theme.argb(down ? 0xE0 : 0x70, 0xFFFFFF), 1);
            Ui.centered(g, label, x + w / 2, y + (h - 8) / 2, down && !Theme.glass ? 0xFF101018 : textColor());
        }

        @Override protected void draw(GuiGraphics g, Minecraft mc, int w, int h) {
            var o = mc.options;
            int x = 4, y = 3;
            key(g, x + 20, y, 18, 18, "W", o.keyUp.isDown());
            y += 20;
            key(g, x, y, 18, 18, "A", o.keyLeft.isDown());
            key(g, x + 20, y, 18, 18, "S", o.keyDown.isDown());
            key(g, x + 40, y, 18, 18, "D", o.keyRight.isDown());
            y += 20;
            if (space.value) {
                key(g, x, y, 58, 12, "Space", o.keyJump.isDown());
                y += 14;
            }
            if (mouse.value) {
                key(g, x, y, 28, 16, "L " + Input.cps(0), o.keyAttack.isDown());
                key(g, x + 30, y, 28, 16, "R " + Input.cps(1), o.keyUse.isDown());
            }
        }
    }

    // ------------------------------------------------------------------ utility

    public static final class ToggleSprint extends Module {
        public ToggleSprint() {
            super("togglesprint", "Toggle Sprint", "Utility", "Sprints automatically while you move forward.", false);
        }

        @Override public void tick(Minecraft mc) {
            if (mc.player == null || mc.screen != null) return;
            if (mc.options.keyUp.isDown() && !mc.player.isShiftKeyDown()) mc.options.keySprint.setDown(true);
        }
    }
}
