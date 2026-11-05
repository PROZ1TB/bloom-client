package dev.bloom;

import com.mojang.blaze3d.platform.InputConstants;
import dev.bloom.module.ModuleManager;
import dev.bloom.screen.BloomScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class BloomClient implements ClientModInitializer {
    private static KeyMapping menuKey;

    /** Optional groups are loaded by name: if one was left out of the build or fails, the rest still works. */
    private static void loadGroup(String cls) {
        try {
            ((Group) Class.forName(cls).getDeclaredConstructor().newInstance()).init();
            System.out.println("[Bloom] loaded " + cls);
        } catch (ClassNotFoundException e) {
            System.out.println("[Bloom] not included in this build: " + cls);
        } catch (Throwable t) {
            System.out.println("[Bloom] could not start " + cls + ": " + t);
        }
    }

    @Override
    public void onInitializeClient() {
        Config.load();
        Theme.load();
        ModuleManager.init();
        for (String cls : new String[] {
                "dev.bloom.risky.sky.SkyGroup", "dev.bloom.risky.glow.GlowGroup",
                "dev.bloom.risky.skin.SkinGroup", "dev.bloom.risky.menu.MenuGroup" }) {
            loadGroup(cls);
        }

        var hudId = VanillaHudElements.MISC_OVERLAYS.withPath("bloom_hud");
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, hudId,
                (graphics, tickCounter) -> HudRenderer.render(graphics));

        var category = KeyMapping.Category.register(VanillaHudElements.MISC_OVERLAYS.withPath("bloom_keys"));
        menuKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.bloomclient.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (menuKey.consumeClick()) {
                if (mc.screen == null) mc.setScreen(new BloomScreen());
            }
            ModuleManager.tick(mc);
            Config.tick();
        });
    }
}
