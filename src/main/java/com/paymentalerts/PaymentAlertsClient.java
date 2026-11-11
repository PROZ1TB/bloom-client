package com.paymentalerts;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

public class PaymentAlertsClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("paymentalerts");
    public static final AlertManager MANAGER = new AlertManager();
    public static Config CONFIG = new Config();

    @Override
    public void onInitializeClient() {
        CONFIG = Config.load();

        // Draw on top of the whole HUD (just under chat).
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.of("paymentalerts", "alerts"),
                (context, tickCounter) -> {
                    if (!CONFIG.enabled) return;
                    MANAGER.render(context, context.getScaledWindowWidth(), context.getScaledWindowHeight());
                });

        // Server / system messages (this is where "<name> paid you $10M" normally arrives).
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) onMessage(message.getString());
        });
        // Real player chat can be faked by anyone, so it is off unless enabled in the config.
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, ts) -> {
            if (CONFIG.listenToChat) onMessage(message.getString());
        });

        registerCommands();
        LOG.info("Payment Alerts loaded. Try /payalert demo");
    }

    static void onMessage(String text) {
        if (!CONFIG.enabled) return;
        Amounts.Payment p = Amounts.parse(text, CONFIG.pattern);
        if (p == null) return;
        Tier tier = Tier.forAmount(p.amount());
        if (tier == null) return;
        int variation = ThreadLocalRandom.current().nextInt(tier.variations);
        MANAGER.submit(new Alert(tier, variation, p.player(), Amounts.format(p.amount())));
    }

    private static String selfName() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) return mc.player.getName().getString();
        return "cobhaa";
    }

    private static int test(FabricClientCommandSource src, String amountText, int variation, String player) {
        double amount;
        try {
            amount = Amounts.parseAmount(amountText);
        } catch (Exception e) {
            src.sendFeedback(Text.literal("Bad amount. Examples: 10M, 50M, 1.5B, 100B"));
            return 0;
        }
        Tier tier = Tier.forAmount(amount);
        if (tier == null) {
            src.sendFeedback(Text.literal("Alerts start at $10M."));
            return 0;
        }
        int v = variation > 0 ? variation - 1 : ThreadLocalRandom.current().nextInt(tier.variations);
        MANAGER.submit(new Alert(tier, v, player, Amounts.format(amount)));
        src.sendFeedback(Text.literal("Playing " + tier.label + " (variation " + (Math.floorMod(v, tier.variations) + 1) + "/" + tier.variations + ")"));
        return 1;
    }

    private static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("payalert")
                        .then(ClientCommandManager.literal("test")
                                .then(ClientCommandManager.argument("amount", StringArgumentType.word())
                                        .executes(x -> test(x.getSource(), StringArgumentType.getString(x, "amount"), 0, selfName()))
                                        .then(ClientCommandManager.argument("variation", IntegerArgumentType.integer(1, 6))
                                                .executes(x -> test(x.getSource(), StringArgumentType.getString(x, "amount"),
                                                        IntegerArgumentType.getInteger(x, "variation"), selfName()))
                                                .then(ClientCommandManager.argument("player", StringArgumentType.word())
                                                        .executes(x -> test(x.getSource(), StringArgumentType.getString(x, "amount"),
                                                                IntegerArgumentType.getInteger(x, "variation"),
                                                                StringArgumentType.getString(x, "player")))))))
                        .then(ClientCommandManager.literal("demo")
                                .executes(x -> {
                                    for (Tier t : Tier.values()) {
                                        MANAGER.submit(new Alert(t, 0, selfName(), Amounts.format(t.min)));
                                    }
                                    x.getSource().sendFeedback(Text.literal("Queued every tier."));
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("toggle")
                                .executes(x -> {
                                    CONFIG.enabled = !CONFIG.enabled;
                                    CONFIG.save();
                                    x.getSource().sendFeedback(Text.literal("Payment Alerts " + (CONFIG.enabled ? "enabled" : "disabled")));
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("clear")
                                .executes(x -> {
                                    MANAGER.clear();
                                    x.getSource().sendFeedback(Text.literal("Cleared."));
                                    return 1;
                                }))
        ));
    }
}
