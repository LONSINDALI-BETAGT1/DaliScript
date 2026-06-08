package com.dali.daliscript;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;

public class DaliScriptMod implements ModInitializer {

    // ⏰ SYSTEMS
    private static final Map<UUID, Long> alarms = new HashMap<>();
    private static final Map<UUID, String> notes = new HashMap<>();
    private static final Map<UUID, Integer> balance = new HashMap<>();

    @Override
    public void onInitialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // 💀 MAIN OS COMMAND
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("dalios")
                    .executes(ctx -> {

                        ServerPlayerEntity p = ctx.getSource().getPlayer();

                        send(p,
                            "💀📱 DaliOS V29\n" +
                            "🧠 /ai ask <question>\n" +
                            "🛒 /shop\n" +
                            "⏰ /alarm\n" +
                            "📝 /notes\n" +
                            "👾 /mobs\n" +
                            "💎 /balance"
                        );

                        return 1;
                    })
            );

            // 🧠 AI SYSTEM
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("ai")
                    .then(net.minecraft.server.command.CommandManager.literal("ask")
                        .then(net.minecraft.server.command.CommandManager.argument("q",
                            com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                            .executes(ctx -> {

                                ServerPlayerEntity p = ctx.getSource().getPlayer();
                                String q = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "q").toLowerCase();

                                send(p, "🧠 DaliAI:");

                                if (q.contains("diamond")) {
                                    send(p, "💎 Best level: Y -58 (simulation knowledge)");
                                } else if (q.contains("mob")) {
                                    send(p, "👾 Strongest: Warden > Wither > Dragon");
                                } else if (q.contains("command")) {
                                    send(p, "⚙ Use /gamemode 0-3 or /dalios");
                                } else {
                                    send(p, "🤖 I am DaliAI offline system. Try ores, mobs, commands.");
                                }

                                return 1;
                            })))
            );

            // ⏰ ALARM
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("alarm")
                    .then(net.minecraft.server.command.CommandManager.argument("sec",
                        com.mojang.brigadier.arguments.IntegerArgumentType.integer(5, 3600))
                        .executes(ctx -> {

                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            int sec = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "sec");

                            alarms.put(p.getUuid(), System.currentTimeMillis() + (sec * 1000L));

                            send(p, "⏰ Alarm set: " + sec + " seconds");

                            return 1;
                        }))
            );

            // 📝 NOTES
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("notes")
                    .then(net.minecraft.server.command.CommandManager.argument("text",
                        com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                        .executes(ctx -> {

                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            String text = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "text");

                            notes.put(p.getUuid(), text);

                            send(p, "📝 Saved note");

                            return 1;
                        }))
            );

            // 💎 BALANCE
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("balance")
                    .executes(ctx -> {

                        ServerPlayerEntity p = ctx.getSource().getPlayer();

                        send(p, "💎 Balance: " + balance.getOrDefault(p.getUuid(), 0));

                        return 1;
                    })
            );
        });

        // 💀 SYSTEM ENGINE
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    private void tick(MinecraftServer server) {

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {

            UUID id = p.getUuid();

            // ⏰ ALARM ENGINE
            if (alarms.containsKey(id)) {
                if (System.currentTimeMillis() >= alarms.get(id)) {
                    send(p, "⏰ ALARM TRIGGERED!");
                    alarms.remove(id);
                }
            }

            // 📡 SYSTEM NOTIFICATION
            if (Math.random() < 0.0001) {
                send(p, "📡 DaliOS: System running stable...");
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