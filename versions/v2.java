package com.dali.daliscript;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class DaliScriptMod implements ModInitializer {

    private static boolean enabled = false;
    private static boolean locked = false;
    private static final String KEY = "DALI";

    @Override
    public void onInitialize() {

        // 🧠 الأوامر
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // 🔑 أمر المفتاح
            dispatcher.register(
                CommandManager.literal("daliscriptkey")
                    .then(CommandManager.argument("key", StringArgumentType.string())
                        .executes(context -> {

                            String input = StringArgumentType.getString(context, "key");

                            if (input.equals(KEY)) {
                                locked = false;
                                context.getSource().sendFeedback(
                                        () -> Text.literal("§aDaliScript UNLOCKED"),
                                        false
                                );
                            } else {
                                locked = true;
                                context.getSource().sendFeedback(
                                        () -> Text.literal("§cWRONG KEY - SYSTEM LOCKED"),
                                        false
                                );
                            }

                            return 1;
                        }))
            );

            // ⚙️ أمر التحكم الرئيسي
            dispatcher.register(
                CommandManager.literal("daliscript")
                    .executes(context -> {

                        if (locked) {
                            context.getSource().sendFeedback(
                                    () -> Text.literal("§4SYSTEM LOCKED - USE /daliscriptkey"),
                                    false
                            );
                            return 0;
                        }

                        enabled = !enabled;

                        context.getSource().sendFeedback(
                                () -> Text.literal(enabled ? "§aDaliScript ENABLED" : "§cDaliScript DISABLED"),
                                false
                        );

                        return 1;
                    })
            );

        });

        // 🛡 Tick system (anti-cheat + hardcore control)
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    private void tick(MinecraftServer server) {

        if (!enabled) return;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

            // 💀 Hardcore control (سلوكي)
            server.getCommandManager().executeWithPrefix(
                    server.getCommandSource(),
                    "difficulty hard"
            );

            server.getCommandManager().executeWithPrefix(
                    server.getCommandSource(),
                    "gamerule keepInventory false"
            );

            // 🛡 Anti-cheat بسيط
            if (player.getAbilities().flying) {
                player.getAbilities().flying = false;
                player.sendMessage(Text.literal("§cFlying blocked by DaliScript"), false);
            }

            // إجبار Survival (اختياري قوي)
            if (player.isCreative()) {
                player.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);
            }
        }
    }
}