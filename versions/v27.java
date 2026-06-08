package com.dali.daliscript;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;

public class DaliScriptMod implements ModInitializer {

    // ⏰ ALARM SYSTEM
    private static final Map<UUID, Long> alarms = new HashMap<>();

    // 🧠 REMINDERS
    private static final Map<UUID, Integer> reminderCounter = new HashMap<>();

    @Override
    public void onInitialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // ⏰ SET ALARM
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("alarm")
                    .then(net.minecraft.server.command.CommandManager.argument("seconds",
                        com.mojang.brigadier.arguments.IntegerArgumentType.integer(5, 3600))
                        .executes(ctx -> {

                            ServerPlayerEntity p = ctx.getSource().getPlayer();

                            int sec = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "seconds");

                            alarms.put(p.getUuid(), System.currentTimeMillis() + (sec * 1000L));

                            send(p, "⏰ Alarm set for " + sec + " seconds");

                            return 1;
                        }))
            );

            // 🧠 DALI AI (FAKE OPENAI)
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("ai")
                    .then(net.minecraft.server.command.CommandManager.literal("ask")
                        .then(net.minecraft.server.command.CommandManager.argument("q",
                            com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                            .executes(ctx -> {

                                ServerPlayerEntity p = ctx.getSource().getPlayer();

                                String q = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "q").toLowerCase();

                                send(p, "🧠 DaliAI:");

                                // 🪨 ORES
                                if (q.contains("diamond") || q.contains("ore")) {
                                    send(p, "💎 Try mining at Y=-58 to -64 (simulation tip)");
                                }

                                // 👾 MOBS
                                else if (q.contains("mob")) {
                                    send(p, "👾 Strong mobs: Warden > Wither > Ender Dragon");
                                }

                                // 📍 COORDS (FAKE SAFE)
                                else if (q.contains("where")) {
                                    send(p, "🧭 I cannot give exact coords, but explore caves below Y=0");
                                }

                                // 🎮 HELP
                                else if (q.contains("gamemode")) {
                                    send(p, "🎮 Use /gamemode 0-3 to change mode");
                                }

                                else {
                                    send(p, "🤖 I am DaliAI (offline model). Try asking about ores, mobs, or commands.");
                                }

                                return 1;
                            })))
            );

            // 👾 MOBS INFO
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("mobs")
                    .executes(ctx -> {

                        ServerPlayerEntity p = ctx.getSource().getPlayer();

                        send(p,
                            "👾 Mob List:\n" +
                            "- Zombie\n" +
                            "- Skeleton\n" +
                            "- Creeper\n" +
                            "- Warden (Boss)\n" +
                            "- Ender Dragon"
                        );

                        return 1;
                    })
            );

            // 💎 ORES HELP
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("ores")
                    .executes(ctx -> {

                        ServerPlayerEntity p = ctx.getSource().getPlayer();

                        send(p,
                            "💎 Ore Guide:\n" +
                            "- Diamond: Y -58\n" +
                            "- Iron: Y 16\n" +
                            "- Gold: Badlands best\n" +
                            "- Ancient Debris: Nether"
                        );

                        return 1;
                    })
            );

        });

        // ⏰ ALARM ENGINE + REMINDER SYSTEM
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    private void tick(net.minecraft.server.MinecraftServer server) {

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {

            UUID id = p.getUuid();

            // ⏰ ALARM CHECK
            if (alarms.containsKey(id)) {

                long time = alarms.get(id);

                if (System.currentTimeMillis() >= time) {

                    send(p, "⏰ ALARM TRIGGERED!");

                    alarms.remove(id);
                }
            }

            // 📡 AUTO REMINDER (feature)
            int count = reminderCounter.getOrDefault(id, 0) + 1;
            reminderCounter.put(id, count);

            if (count % 6000 == 0) { // كل فترة طويلة
                send(p, "📡 Reminder: Stay active in your world!");
            }
        }
    }

    // 💬 SEND
    private void send(ServerPlayerEntity p, String msg) {
        if (p != null) {
            p.sendMessage(Text.literal(msg), false);
        }
    }
}