package com.dali.daliscript;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import java.util.*;

public class DaliScriptMod implements ModInitializer {

    private static final Map<UUID, Long> alarms = new HashMap<>();
    private static final Map<UUID, String> notes = new HashMap<>();
    private static final Map<UUID, Integer> bank = new HashMap<>();
    private static final Map<UUID, List<String>> memory = new HashMap<>();

    @Override
    public void onInitialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // 💀 MAIN MENU
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("dalios")
                    .executes(ctx -> {
                        send(ctx.getSource().getPlayer(),
                                "💀📱 DaliOS V30\n" +
                                "/ai ask | /bank | /loan | /shop | /notes | /alarm | /mobs | /ores | /balance");
                        return 1;
                    })
            );

            // 🧠 AI
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("ai")
                    .then(net.minecraft.server.command.CommandManager.literal("ask")
                        .then(net.minecraft.server.command.CommandManager.argument("q",
                                StringArgumentType.greedyString())
                            .executes(ctx -> {

                                ServerPlayerEntity p = ctx.getSource().getPlayer();
                                String q = StringArgumentType.getString(ctx, "q").toLowerCase();

                                memory.computeIfAbsent(p.getUuid(), k -> new ArrayList<>()).add(q);

                                if (q.contains("diamond")) {
                                    send(p, "💎 Try Y -58 for diamonds");
                                } else if (q.contains("mob")) {
                                    send(p, "👾 Strongest mob: Warden");
                                } else {
                                    send(p, "🧠 AI: I learned your question.");
                                }

                                return 1;
                            })))
            );

            // 🏦 BANK BALANCE
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("bank")
                    .then(net.minecraft.server.command.CommandManager.literal("balance")
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            send(p, "🏦 Bank: " + bank.getOrDefault(p.getUuid(), 0));
                            return 1;
                        }))
            );

            // 🏦 BANK DEPOSIT
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("bank")
                    .then(net.minecraft.server.command.CommandManager.literal("deposit")
                        .then(net.minecraft.server.command.CommandManager.argument("amount",
                                IntegerArgumentType.integer(1))
                            .executes(ctx -> {

                                ServerPlayerEntity p = ctx.getSource().getPlayer();
                                int amt = IntegerArgumentType.getInteger(ctx, "amount");

                                bank.put(p.getUuid(),
                                        bank.getOrDefault(p.getUuid(), 0) + amt);

                                send(p, "💰 Deposited: " + amt);

                                return 1;
                            })))
            );

            // 💳 LOAN
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("loan")
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        bank.put(p.getUuid(), bank.getOrDefault(p.getUuid(), 0) + 100);
                        send(p, "💳 Loan approved: +100");
                        return 1;
                    })
            );

            // 🛒 SHOP
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("shop")
                    .executes(ctx -> {
                        send(ctx.getSource().getPlayer(),
                                "🛒 Shop:\nNitro = 32 Emerald\nPhone = 3 Netherite");
                        return 1;
                    })
            );

            // 📝 NOTES
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("notes")
                    .then(net.minecraft.server.command.CommandManager.argument("text",
                            StringArgumentType.greedyString())
                        .executes(ctx -> {

                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            String text = StringArgumentType.getString(ctx, "text");

                            notes.put(p.getUuid(), text);

                            send(p, "📝 Note saved");

                            return 1;
                        }))
            );

            // ⏰ ALARM
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("alarm")
                    .then(net.minecraft.server.command.CommandManager.argument("sec",
                            IntegerArgumentType.integer(5, 3600))
                        .executes(ctx -> {

                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            int sec = IntegerArgumentType.getInteger(ctx, "sec");

                            alarms.put(p.getUuid(), System.currentTimeMillis() + (sec * 1000L));

                            send(p, "⏰ Alarm set: " + sec + "s");

                            return 1;
                        }))
            );

            // 👾 MOBS
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("mobs")
                    .executes(ctx -> {
                        send(ctx.getSource().getPlayer(),
                                "👾 Zombie | Skeleton | Creeper | Warden");
                        return 1;
                    })
            );

            // 💎 ORES
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("ores")
                    .executes(ctx -> {
                        send(ctx.getSource().getPlayer(),
                                "💎 Diamond Y=-58 | Iron Y=16 | Netherite Nether");
                        return 1;
                    })
            );

            // 💰 BALANCE
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("balance")
                    .executes(ctx -> {
                        send(ctx.getSource().getPlayer(),
                                "💰 " + bank.getOrDefault(ctx.getSource().getPlayer().getUuid(), 0));
                        return 1;
                    })
            );

        });

        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    // ⏰ ENGINE
    private void tick(MinecraftServer server) {

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {

            UUID id = p.getUuid();

            if (alarms.containsKey(id)) {
                if (System.currentTimeMillis() >= alarms.get(id)) {
                    send(p, "⏰ ALARM TRIGGERED!");
                    alarms.remove(id);
                }
            }
        }
    }

    private void send(ServerPlayerEntity p, String msg) {
        if (p != null) {
            p.sendMessage(Text.literal(msg), false);
        }
    }
}