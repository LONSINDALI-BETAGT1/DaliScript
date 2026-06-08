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

    // 💀 CORE SYSTEM
    private static final Set<UUID> nitro = new HashSet<>();
    private static final Map<UUID, Integer> warnings = new HashMap<>();

    // 📞 ROLEPLAY NUMBERS (Dali Numbers)
    private static final Map<UUID, String> phoneNumbers = new HashMap<>();

    @Override
    public void onInitialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // 📱 DaliPhone MENU
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("daliphone")
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();

                        send(p,
                            "§b📱 DaliPhone V21\n" +
                            "§7/dalicord\n" +
                            "§7/dalimessage\n" +
                            "§7/dalishop\n" +
                            "§7/number\n" +
                            "§7/ai trader"
                        );

                        return 1;
                    })
            );

            // 💬 DALICORD
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("dalicord")
                    .then(net.minecraft.server.command.CommandManager.literal("buy")
                        .then(net.minecraft.server.command.CommandManager.literal("nitro")
                            .executes(ctx -> {

                                ServerPlayerEntity p = ctx.getSource().getPlayer();

                                nitro.add(p.getUuid());

                                send(p, "§d💎 Nitro Activated (32 Diamonds)");
                                return 1;
                            })))
            );

            // 📩 DALIMESSAGE (PRIVATE CHAT SIMULATION)
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("dalimessage")
                    .then(net.minecraft.server.command.CommandManager.literal("send")
                        .then(net.minecraft.server.command.CommandManager.argument("msg",
                            com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                            .executes(ctx -> {

                                ServerPlayerEntity p = ctx.getSource().getPlayer();

                                String msg = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "msg");

                                send(p, "§a📩 Message Sent (SIM): " + msg);
                                send(p, "§7AI Response: I received your message.");

                                return 1;
                            })))
            );

            // 🧠 AI TRADER (SIMULATED)
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("ai")
                    .then(net.minecraft.server.command.CommandManager.literal("trader")
                        .executes(ctx -> {

                            ServerPlayerEntity p = ctx.getSource().getPlayer();

                            send(p,
                                "§6🧠 AI TRADER\n" +
                                "- Nitro = 32 Diamonds\n" +
                                "- Phone Number = 3 Netherite Blocks\n" +
                                "- Status: ACTIVE"
                            );

                            return 1;
                        }))
            );

            // 📞 BUY PHONE NUMBER
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("number")
                    .executes(ctx -> {

                        ServerPlayerEntity p = ctx.getSource().getPlayer();

                        String number = "+2699 00 00 0000";
                        phoneNumbers.put(p.getUuid(), number);

                        send(p,
                            "§e📞 New Number Assigned:\n" +
                            number + "\n" +
                            "Cost: 3 Netherite Blocks (SIM)"
                        );

                        return 1;
                    })
            );

            // 🎮 GAMEMODE CONTROL (optional system inside same mod)
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

            // 🛡 SIMPLE ANTI CHEAT (SIMULATION)
            if (!nitro.contains(p.getUuid())) {

                if (p.getAbilities().flying) {
                    p.getAbilities().flying = false;
                    warn(p, "FLY DETECTED");
                }

                if (p.getVelocity().length() > 1.25) {
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
            p.networkHandler.disconnect(Text.literal("BANNED BY DaliScript V21"));
        }
    }

    // 💬 SEND
    private void send(ServerPlayerEntity p, String msg) {
        if (p != null) {
            p.sendMessage(Text.literal(msg), false);
        }
    }
}