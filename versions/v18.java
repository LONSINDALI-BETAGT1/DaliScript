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

    // 💀 CORE STATE
    private static boolean systemOn = true;

    private static UUID owner = null;

    private static final Set<UUID> admins = new HashSet<>();
    private static final Set<UUID> nitro = new HashSet<>();
    private static final Map<UUID, Integer> warnings = new HashMap<>();

    @Override
    public void onInitialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // 👑 OWNER KEY
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("daliscriptkey")
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        owner = p.getUuid();
                        send(p, "§aDaliScript V19 OWNER ACTIVATED");
                        return 1;
                    })
            );

            // 📱 DALIPHONE
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("daliphone")
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();

                        send(p,
                                "§b📱 DaliPhone V19\n" +
                                "§7/dalicord\n" +
                                "§7/dalishop\n" +
                                "§7/admin\n" +
                                "§7/info"
                        );
                        return 1;
                    })
            );

            // 💬 DALICORD
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("dalicord")
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();

                        String tag = nitro.contains(p.getUuid()) ? "§d[NITRO]" : "§7[USER]";
                        send(p, tag + " Connected to Dalicord");
                        return 1;
                    })
            );

            // 🛒 SHOP (Nitro)
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("dalishop")
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();

                        send(p,
                                "§6🛒 DaliShop\n" +
                                "§dNitro Boost = 32 Diamonds\n" +
                                "Use /buy nitro"
                        );
                        return 1;
                    })
            );

            // 💎 BUY NITRO
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("buy")
                    .then(net.minecraft.server.command.CommandManager.literal("nitro")
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();

                            nitro.add(p.getUuid());
                            send(p, "§dNITRO ACTIVATED (V19)");
                            return 1;
                        }))
            );

            // 🎮 GAMEMODE (Hardcore control)
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("gamemode")
                    .then(net.minecraft.server.command.CommandManager.argument("mode",
                            com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 3))
                        .executes(ctx -> {

                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            int m = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "mode");

                            GameMode gm = switch (m) {
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
            );

        });

        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    // 💀 ENGINE
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

            // 🛡 ANTI CHEAT ENGINE
            if (!nitro.contains(p.getUuid())) {

                if (p.getAbilities().flying) {
                    p.getAbilities().flying = false;
                    warn(p, "FLY DETECTED");
                }

                if (p.getVelocity().length() > 1.3) {
                    p.setVelocity(0, 0, 0);
                    warn(p, "SPEED ANOMALY");
                }

                if (p.isCreative()) {
                    p.changeGameMode(GameMode.SURVIVAL);
                    warn(p, "CREATIVE BLOCKED");
                }
            }
        }
    }

    // ⚠️ WARN SYSTEM
    private void warn(ServerPlayerEntity p, String reason) {
        UUID id = p.getUuid();

        int w = warnings.getOrDefault(id, 0) + 1;
        warnings.put(id, w);

        send(p, "§c[WARN] " + reason + " (" + w + "/5)");

        if (w >= 5) {
            p.networkHandler.disconnect(Text.literal("BANNED BY DaliScript V19"));
        }
    }

    // 💬 SEND
    private void send(ServerPlayerEntity p, String msg) {
        if (p != null) p.sendMessage(Text.literal(msg), false);
    }
}