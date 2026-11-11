package com.paymentalerts;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Queues alerts: one big full-screen effect at a time, toasts stack in the top-right. */
public final class AlertManager {
    private static final int MAX_QUEUE = 16;

    private final ArrayDeque<Alert> queue = new ArrayDeque<>();
    private final List<Alert> toasts = new ArrayList<>();
    private Alert current;

    public void submit(Alert a) {
        long now = System.currentTimeMillis();
        a.startMs = now;
        if (a.tier == Tier.SKIN) {
            toasts.add(a);
        } else if (queue.size() < MAX_QUEUE) {
            queue.add(a);
        }
    }

    public void clear() {
        queue.clear();
        toasts.clear();
        current = null;
    }

    public void render(DrawContext c, int w, int h) {
        long now = System.currentTimeMillis();

        if (current != null && current.age(now) > current.tier.duration) current = null;
        if (current == null && !queue.isEmpty()) {
            current = queue.poll();
            current.startMs = now;
            current.soundIdx = 0;
            if (current.tier == Tier.MONEY) {
                Alert toast = new Alert(Tier.MONEY, current.variation, current.player, current.amountText);
                toast.startMs = now;
                toast.soundIdx = Integer.MAX_VALUE; // the big effect already plays the sounds
                toasts.add(toast);
            }
        }
        if (current != null) {
            float t = current.age(now);
            playSounds(current, t);
            Effects.render(c, current, w, h, t);
        }

        toasts.removeIf(x -> x.age(System.currentTimeMillis()) > x.tier.duration);
        int idx = 0;
        for (Alert ta : toasts) {
            float age = ta.age(now);
            playSounds(ta, age);
            Effects.toast(c, ta, w, h, idx++, age, ta.tier.duration);
        }
    }

    private void playSounds(Alert a, float t) {
        Effects.Snd[] list = Effects.sounds(a.tier);
        while (a.soundIdx < list.length && list[a.soundIdx].time() <= t) {
            Effects.Snd s = list[a.soundIdx];
            a.soundIdx++;
            play(s);
        }
    }

    private static void play(Effects.Snd s) {
        if (PaymentAlertsClient.CONFIG != null && !PaymentAlertsClient.CONFIG.sounds) return;
        try {
            SoundEvent ev = SoundEvent.of(Identifier.ofVanilla(s.id()));
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(ev, s.pitch(), s.vol()));
        } catch (Exception ignored) {
            // never let a sound problem break rendering
        }
    }
}
