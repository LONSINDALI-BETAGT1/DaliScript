package com.dali.daliscript;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.util.HashSet;
import java.util.Set;

public class DaliScriptMod implements ModInitializer {

    private static boolean enabled = false;
    private static boolean locked = true;
    private static final String KEY = "DALI";

    private static final Set<String> log = new HashSet<>();

    @Override
    public void onInitialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // 🔑 المفتاح
            dispatcher.register(
                CommandManager.literal("daliscriptkey")
                    .then(CommandManager.argument("key", StringArgumentType.string())
                        .executes(ctx -> {

                            String input = StringArgumentType.getString(ctx, "key");

                            if (input.equals(KEY)) {
                                locked = false;
                                ctx.getSource().sendFeedback(() ->
                                        Text.literal("§aDaliScript UNLOCKED"), false);
                            } else {
                                locked = true;
                                ctx.getSource().sendFeedback(() ->
                                        Text.literal("§4WRONG KEY → SYSTEM LOCKED"), false);
                            }

                            return 1;
                        }))
            );

            // ⚙️ تشغيل / إيقاف
            dispatcher.register(
                CommandManager.literal("daliscript")
                    .executes(ctx -> {

                        if (locked) {
                            ctx.getSource().sendFeedback(() ->
                                    Text.literal("§cLOCKED - USE /daliscriptkey"), false);
                            return 0;
                        }

                        enabled = !enabled;

                        log.add("SYSTEM " + (enabled ? "ENABLED" : "DISABLED"));

                        ctx.getSource().sendFeedback(() ->
                                Text.literal(enabled ? "§aDaliScript ACTIVE" : "§cDaliScript OFF"), false);

                        return 1;
                    })
            );

        });

        // 🛡 Tick system
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    private void tick(MinecraftServer server) {

        if (!enabled) return;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

            // 💀 Hardcore enforcement
            server.getCommandManager().executeWithPrefix(
                    server.getCommandSource(),
                    "difficulty hard"
            );

            server.getCommandManager().executeWithPrefix(
                    server.getCommandSource(),
                    "gamerule keepInventory false"
            );

            server.getCommandManager().executeWithPrefix(
                    server.getCommandSource(),
                    "gamerule doImmediateRespawn false"
            );

            // 🛡 Anti-cheat core

            // منع creative
            if (player.isCreative()) {
                player.changeGameMode(GameMode.SURVIVAL);
                log.add(player.getName().getString() + " forced to SURVIVAL");
            }

            // منع الطيران
            if (player.getAbilities().flying) {
                player.getAbilities().flying = false;
                player.sendMessage(Text.literal("§cFly blocked"), false);
            }

            // speed hack بسيط (تقريبي)
            if (player.getVelocity().length() > 1.2) {
                player.setVelocity(0, 0, 0);
                player.sendMessage(Text.literal("§cSpeed blocked"), false);
            }
        }
    }
}