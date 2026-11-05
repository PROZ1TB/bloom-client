package dev.bloom.module;

import dev.bloom.Config;
import dev.bloom.Theme;

/** One option of a module. Saved automatically to the config when changed. */
public abstract class Setting {
    public final String id, name;
    String key;

    protected Setting(String id, String name) {
        this.id = id;
        this.name = name;
    }

    protected abstract String encode();
    protected abstract void decode(String s);

    protected void changed() {
        if (key != null) Config.put(key, encode());
    }

    // ------------------------------------------------------------------

    public static final class Bool extends Setting {
        public boolean value;

        public Bool(String id, String name, boolean def) {
            super(id, name);
            this.value = def;
        }

        public void toggle() { value = !value; changed(); }
        @Override protected String encode() { return Boolean.toString(value); }
        @Override protected void decode(String s) { value = Boolean.parseBoolean(s); }
    }

    public static final class Num extends Setting {
        public double value;
        public final double min, max, step;

        public Num(String id, String name, double min, double max, double step, double def) {
            super(id, name);
            this.min = min;
            this.max = max;
            this.step = step;
            this.value = def;
        }

        public void set(double v) {
            v = Math.max(min, Math.min(max, v));
            if (step > 0) v = Math.round(v / step) * step;
            value = Math.max(min, Math.min(max, v));
            changed();
        }

        public void setFraction(double f) { set(min + (max - min) * Math.max(0, Math.min(1, f))); }
        public double fraction() { return (value - min) / (max - min); }
        public int intValue() { return (int) Math.round(value); }

        public String display() {
            return step >= 1 ? Integer.toString((int) Math.round(value)) : String.format("%.2f", value);
        }

        @Override protected String encode() { return Double.toString(value); }
        @Override protected void decode(String s) { value = Math.max(min, Math.min(max, Double.parseDouble(s))); }
    }

    public static final class Mode extends Setting {
        public int index;
        public final String[] modes;

        public Mode(String id, String name, int def, String... modes) {
            super(id, name);
            this.modes = modes;
            this.index = def;
        }

        public void cycle() { index = (index + 1) % modes.length; changed(); }
        public String get() { return modes[index]; }
        @Override protected String encode() { return Integer.toString(index); }
        @Override protected void decode(String s) { index = Math.max(0, Math.min(modes.length - 1, Integer.parseInt(s))); }
    }

    public static final class Color extends Setting {
        public int rgb;

        public Color(String id, String name, int def) {
            super(id, name);
            this.rgb = def & 0xFFFFFF;
        }

        public void cycle() {
            int cur = -1;
            for (int i = 0; i < Theme.PALETTE.length; i++) if (Theme.PALETTE[i] == rgb) cur = i;
            rgb = Theme.PALETTE[(cur + 1) % Theme.PALETTE.length];
            changed();
        }

        public int argb() { return 0xFF000000 | rgb; }
        @Override protected String encode() { return Integer.toHexString(rgb); }
        @Override protected void decode(String s) { rgb = Integer.parseInt(s, 16) & 0xFFFFFF; }
    }
}
