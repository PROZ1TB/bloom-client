package dev.bloom.risky.skin;

import dev.bloom.visual.Avatar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;

/** Paints the menu character with your real Minecraft skin. Falls back to the blocky avatar if anything fails. */
public final class SkinPainter implements Avatar.Painter {
    @Override
    public void paint(GuiGraphics g, int x, int y, int u, double walk, boolean moving, double lookX, double lookY, double time) {
        Minecraft mc = Minecraft.getInstance();
        var skin = mc.getSkinManager().getInsecureSkin(mc.getGameProfile());
        var tex = skin.texture();
        Avatar.Pose p = Avatar.Pose.of(walk, moving, lookX, lookY, time);

        int bob = (int) Math.round(p.bob * u);
        int bx = x + (int) Math.round(p.bodyX * u);
        int legTop = y - 12 * u - bob;
        int bodyTop = y - 24 * u - bob;
        int hx = bx - 4 * u + (int) Math.round(p.headX * u), hy = y - 32 * u - bob + (int) Math.round(p.headY * u);
        int lhL = 12 * u - (int) Math.round(p.legL * u) + bob, lhR = 12 * u - (int) Math.round(p.legR * u) + bob;
        int aL = (int) Math.round(p.armL * u), aR = (int) Math.round(p.armR * u);

        // {dest x, dest y, dest w, dest h, skin u, skin v, skin w, skin h}, drawn in this order (base layers, then overlays)
        int[][] parts = {
            { bx - 4 * u, legTop, 4 * u, lhL, 4, 20, 4, 12 },        // right leg
            { bx, legTop, 4 * u, lhR, 20, 52, 4, 12 },                // left leg
            { bx - 4 * u, legTop, 4 * u, lhL, 4, 36, 4, 12 },        // right pants
            { bx, legTop, 4 * u, lhR, 4, 52, 4, 12 },                 // left pants
            { bx - 4 * u, bodyTop, 8 * u, 12 * u, 20, 20, 8, 12 },   // body
            { bx - 4 * u, bodyTop, 8 * u, 12 * u, 20, 36, 8, 12 },   // jacket
            { bx - 8 * u, bodyTop + aL, 4 * u, 12 * u, 44, 20, 4, 12 }, // right arm
            { bx - 8 * u, bodyTop + aL, 4 * u, 12 * u, 44, 36, 4, 12 }, // right sleeve
            { bx + 4 * u, bodyTop + aR, 4 * u, 12 * u, 36, 52, 4, 12 }, // left arm
            { bx + 4 * u, bodyTop + aR, 4 * u, 12 * u, 52, 52, 4, 12 }, // left sleeve
            { hx, hy, 8 * u, 8 * u, 8, 8, 8, 8 },                     // head
            { hx, hy, 8 * u, 8 * u, 40, 8, 8, 8 },                    // hat layer
        };
        for (int[] q : parts) {
            if (q[3] <= 0) continue;
            g.blit(RenderPipelines.GUI_TEXTURED, tex, q[0], q[1], (float) q[4], (float) q[5], q[2], q[3], q[6], q[7], 64, 64);
        }
    }
}
