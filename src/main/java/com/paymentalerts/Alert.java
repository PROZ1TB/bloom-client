package com.paymentalerts;

public final class Alert {
    public final Tier tier;
    public final int variation; // 0-based
    public final String player;
    public final String amountText;
    public final long seed;
    public long startMs;
    public int soundIdx = 0;

    public Alert(Tier tier, int variation, String player, String amountText) {
        this.tier = tier;
        this.variation = Math.floorMod(variation, tier.variations);
        this.player = player;
        this.amountText = amountText;
        this.seed = System.nanoTime() ^ ((long) player.hashCode() << 17);
        this.startMs = System.currentTimeMillis();
    }

    public float age(long now) {
        return (now - startMs) / 1000f;
    }
}
