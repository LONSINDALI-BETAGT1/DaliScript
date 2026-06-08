package com.dali.daliscript;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.util.*;

public class DaliScriptMod implements ModInitializer {

    // 💀 CORE
    private static boolean systemOn = true;

    private static final Set<java.util.UUID> nitroUsers = new HashSet<>();
    private static final Map<java.util.UUID, Integer> warnings = new HashMap<>();

    @Override
    public void onInitialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // 💎 NITRO BOOST BUY
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("dalicord")
                    .then(net.minecraft.server.command.CommandManager.literal("nitro")
                        .executes(ctx -> {

                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            if (p == null) return 0;

                            // ⚠️ Simulated economy check
                            // (real implementation requires inventory check)
                            nitroUsers.add(p.getUuid());

                            send(p, "§dNITRO ACTIVATED (32 DIAMONDS)");
                            send(p, "§7You now have premium chat badge");

                            return 1;
                        }))
            );

            // 💬 DALICORD CHAT
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("dalicord")
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        send(p, "§b[DaliCord] Chat System Active");
                        return 1;
                    })
            );

            // 🎮 GAMEMODE CONTROL (حسب ترتيبك)
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("daliscript")
                    .then(net.minecraft.server.command.CommandManager.literal("gamemode")
                        .then(net.minecraft.server.command.CommandManager.argument("mode",
                                com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 3))
                            .executes(ctx -> {

                                ServerPlayerEntity p = ctx.getSource().getPlayer();
                                if (p == null) return 0;

                                int mode = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "mode");

                                GameMode gm = switch (mode) {
                                    case 0 -> GameMode.CREATIVE;
                                    case 1 -> GameMode.SPECTATOR;
                                    case 2 -> GameMode.SURVIVAL;
                                    case 3 -> GameMode.ADVENTURE;
                                    default -> GameMode.SURVIVAL;
                                };

                                p.changeGameMode(gm);
                                send(p, "§aMODE SET: " + gm.name());

                                return 1;
                            }))
                    )
            );

        });

        // 💀 ENGINE LOOP
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    // 💀 CORE SYSTEM
    private void tick(MinecraftServer server) {

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {

            // 💀 HARDCORE RULES
            server.getCommandManager().executeWithPrefix(
                    server.getCommandSource(),
                    "difficulty hard"
            );

            server.getCommandManager().executeWithPrefix(
                    server.getCommandSource(),
                    "gamerule keepInventory false"
            );

            // 🛡 CHEAT CONTROL (REALISTIC)
            if (!nitroUsers.contains(p.getUuid())) {

                // fly detection
                if (p.getAbilities().flying) {
                    p.getAbilities().flying = false;
                    warn(p, "FLY DETECTED");
                }

                // speed detection
                if (p.getVelocity().length() > 1.25) {
                    p.setVelocity(0, 0, 0);
                    warn(p, "SPEED LIMIT EXCEEDED");
                }

                // creative abuse block
                if (p.isCreative()) {
                    p.changeGameMode(GameMode.SURVIVAL);
                    warn(p, "CREATIVE BLOCKED");
                }
            }
        }
    }

    // ⚠️ WARN SYSTEM
    private void warn(ServerPlayerEntity p, String reason) {
        java.util.UUID id = p.getUuid();

        int w = warnings.getOrDefault(id, 0) + 1;
        warnings.put(id, w);

        send(p, "§c[WARN] " + reason + " (" + w + "/5)");

        if (w >= 5) {
            p.networkHandler.disconnect(Text.literal("KICKED BY DaliScript V18"));
        }
    }

    // 💬 SEND
    private void send(ServerPlayerEntity p, String msg) {
        if (p != null) p.sendMessage(Text.literal(msg), false);
    }
}