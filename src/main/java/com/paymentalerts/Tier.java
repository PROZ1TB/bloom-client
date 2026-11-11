package com.paymentalerts;

/** Payment tiers, matching the preview video. */
public enum Tier {
    SKIN(10_000_000d, "$10M - Skin tab", 4, 5.0f),
    MONEY(50_000_000d, "$50M - Money rain + gold tab", 4, 7.5f),
    FIREWORKS(100_000_000d, "$100M - Fireworks", 4, 9.0f),
    LIGHTNING(250_000_000d, "$250M - Lightning storm", 4, 8.5f),
    JACKPOT(500_000_000d, "$500M - Jackpot", 6, 10.5f),
    NUKE(1_000_000_000d, "$1B - Nuke", 4, 10.0f),
    LASER(5_000_000_000d, "$5B - Orbital laser", 5, 8.5f),
    BLACKHOLE(10_000_000_000d, "$10B - Black hole", 4, 11.0f),
    GOD(100_000_000_000d, "$100B - God tier", 4, 11.5f);

    public final double min;
    public final String label;
    public final int variations;
    public final float duration;

    Tier(double min, String label, int variations, float duration) {
        this.min = min;
        this.label = label;
        this.variations = variations;
        this.duration = duration;
    }

    /** Highest tier whose minimum is <= amount, or null if below $10M. */
    public static Tier forAmount(double amount) {
        Tier best = null;
        for (Tier t : values()) {
            if (amount >= t.min) best = t;
        }
        return best;
    }
}
