package dev.bloom.risky.glow;

import dev.bloom.module.Module;
import dev.bloom.module.Setting;
import net.minecraft.client.Minecraft;

/** Gives other players the vanilla glowing outline (visible through walls). */
public final class PlayerGlowModule extends Module {
    private final Setting.Bool self = add(new Setting.Bool("self", "Glow Yourself", false));

    public PlayerGlowModule() {
        super("playerglow", "Player Glow", "Visual", "A glowing outline around players, even through walls.", false);
    }

    @Override
    public void tick(Minecraft mc) {
        if (mc.level == null) return;
        for (var p : mc.level.players()) {
            if (p == mc.player && !self.value) continue;
            p.setGlowingTag(true);
        }
    }

    @Override
    public void setEnabled(boolean on) {
        super.setEnabled(on);
        if (!on) clear();
    }

    private void clear() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (var p : mc.level.players()) p.setGlowingTag(false);
    }
}
