package com.dali.daliscript;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.time.LocalDateTime;
import java.util.*;

public class DaliScriptMod implements ModInitializer {

    // 📝 NOTES STORAGE
    private static final Map<UUID, String> notes = new HashMap<>();

    @Override
    public void onInitialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // 📱 SMART MENU
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("daliscript")
                    .then(net.minecraft.server.command.CommandManager.literal("menu")
                        .executes(ctx -> {

                            ServerPlayerEntity p = ctx.getSource().getPlayer();

                            send(p,
                                "§b📱 DaliSmart Menu V27\n" +
                                "🕒 /clock\n" +
                                "📝 /notes\n" +
                                "🧮 /calc\n" +
                                "🌤 /weather\n" +
                                "📅 /calendar"
                            );

                            return 1;
                        }))
            );

            // 🕒 CLOCK
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("clock")
                    .executes(ctx -> {

                        ServerPlayerEntity p = ctx.getSource().getPlayer();

                        String time = LocalDateTime.now().toLocalTime().toString();

                        send(p, "🕒 Server Time: " + time);

                        return 1;
                    })
            );

            // 📝 NOTES
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("notes")
                    .then(net.minecraft.server.command.CommandManager.literal("set")
                        .then(net.minecraft.server.command.CommandManager.argument("text",
                            com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                            .executes(ctx -> {

                                ServerPlayerEntity p = ctx.getSource().getPlayer();
                                String text = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "text");

                                notes.put(p.getUuid(), text);

                                send(p, "📝 Note saved: " + text);

                                return 1;
                            })))
            );

            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("notes")
                    .executes(ctx -> {

                        ServerPlayerEntity p = ctx.getSource().getPlayer();

                        send(p, "📝 Your Note: " + notes.getOrDefault(p.getUuid(), "Empty"));

                        return 1;
                    })
            );

            // 🧮 CALCULATOR (simple)
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("calc")
                    .then(net.minecraft.server.command.CommandManager.argument("a",
                        com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                        .then(net.minecraft.server.command.CommandManager.argument("op",
                            com.mojang.brigadier.arguments.StringArgumentType.word())
                            .then(net.minecraft.server.command.CommandManager.argument("b",
                                com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                .executes(ctx -> {

                                    ServerPlayerEntity p = ctx.getSource().getPlayer();

                                    int a = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "a");
                                    String op = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "op");
                                    int b = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "b");

                                    int result = 0;

                                    switch (op) {
                                        case "+" -> result = a + b;
                                        case "-" -> result = a - b;
                                        case "*" -> result = a * b;
                                        case "/" -> result = (b != 0) ? a / b : 0;
                                    }

                                    send(p, "🧮 Result: " + result);

                                    return 1;
                                })))))
            );

            // 🌤 WEATHER (SIMULATION)
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("weather")
                    .executes(ctx -> {

                        ServerPlayerEntity p = ctx.getSource().getPlayer();

                        String[] weather = {"Sunny ☀", "Rain 🌧", "Storm ⛈", "Cloudy ☁"};

                        String current = weather[new Random().nextInt(weather.length)];

                        send(p, "🌤 Weather: " + current);

                        return 1;
                    })
            );

            // 📅 CALENDAR
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("calendar")
                    .executes(ctx -> {

                        ServerPlayerEntity p = ctx.getSource().getPlayer();

                        LocalDateTime now = LocalDateTime.now();

                        send(p,
                            "📅 Calendar\n" +
                            "Year: " + now.getYear() + "\n" +
                            "Month: " + now.getMonth() + "\n" +
                            "Day: " + now.getDayOfMonth()
                        );

                        return 1;
                    })
            );

        });
    }

    // 💬 SEND
    private void send(ServerPlayerEntity p, String msg) {
        if (p != null) {
            p.sendMessage(Text.literal(msg), false);
        }
    }
}