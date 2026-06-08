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
    private static final Set<UUID> nitroUsers = new HashSet<>();
    private static final Map<UUID, Integer> warnings = new HashMap<>();

    // 💬 SIMPLE MESSAGE STORAGE
    private static final Map<UUID, List<String>> messages = new HashMap<>();

    @Override
    public void onInitialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // 📱 DALI PHONE
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("daliphone")
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();

                        send(p,
                            "§b📱 DaliPhone V23\n" +
                            "§71. Messages\n" +
                            "§72. Dalicord\n" +
                            "§73. Shop\n" +
                            "§74. AI Trader"
                        );

                        return 1;
                    })
            );

            // 💬 DALI MESSAGE (UI SIMULATION)
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("message")
                    .then(net.minecraft.server.command.CommandManager.literal("send")
                        .then(net.minecraft.server.command.CommandManager.argument("text",
                            com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                            .executes(ctx -> {

                                ServerPlayerEntity p = ctx.getSource().getPlayer();
                                String msg = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "text");

                                messages.computeIfAbsent(p.getUuid(), k -> new ArrayList<>()).add(msg);

                                send(p, "§a📩 Message Saved: " + msg);

                                return 1;
                            })))
            );

            // 🧠 AI TRADER
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("aitrader")
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();

                        send(p,
                            "§6🧠 AI TRADER V23\n" +
                            "- Nitro: 32 Diamonds\n" +
                            "- Number: 3 Netherite Blocks\n" +
                            "- Market: Stable"
                        );

                        return 1;
                    })
            );

            // 🛒 SHOP
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("shop")
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();

                        send(p,
                            "§6🛒 SHOP\n" +
                            "- Nitro = 32 Diamonds\n" +
                            "- Phone Number = 3 Netherite Blocks"
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

                            nitroUsers.add(p.getUuid());

                            send(p, "§d💎 Nitro Activated (V23)");

                            return 1;
                        }))
            );

            // 🎮 GAMEMODE CONTROL
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
                            send(p, "§aMODE: " + gm.name());

                            return 1;
                        }))
            );
        });

        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    // 💀 CORE ENGINE
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

            // 🛡 ANTI CHEAT V23
            if (!nitroUsers.contains(p.getUuid())) {

                if (p.getAbilities().flying) {
                    p.getAbilities().flying = false;
                    warn(p, "FLY DETECTED");
                }

                if (p.getVelocity().length() > 1.3) {
                    p.setVelocity(0, 0, 0);
                    warn(p, "SPEED DETECTED");
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
            p.networkHandler.disconnect(Text.literal("BANNED BY DaliScript V23"));
        }
    }

    // 💬 SEND
    private void send(ServerPlayerEntity p, String msg) {
        if (p != null) {
            p.sendMessage(Text.literal(msg), false);
        }
    }
}