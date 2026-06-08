package com.dali.daliscript;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;

public class DaliScriptMod implements ModInitializer {

    // 🚨 WARNING SYSTEM
    private static final Map<UUID, Integer> warnings = new HashMap<>();

    // 🔒 LOCK SYSTEM
    private static final Map<UUID, Boolean> locked = new HashMap<>();

    // 🚫 WORD FILTER LIST (بسيطة للتجربة)
    private static final List<String> bannedWords = Arrays.asList(
            "hack",
            "cheat",
            "exploit",
            "killall",
            "grief"
    );

    @Override
    public void onInitialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // 🧠 AI SAFE ASK
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("ai")
                    .then(net.minecraft.server.command.CommandManager.literal("ask")
                        .then(net.minecraft.server.command.CommandManager.argument("q",
                                com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                            .executes(ctx -> {

                                ServerPlayerEntity p = ctx.getSource().getPlayer();
                                String msg = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "q").toLowerCase();

                                if (isBanned(msg)) {
                                    punish(p, msg);
                                    return 1;
                                }

                                p.sendMessage(Text.literal("🧠 AI: Safe response generated"), false);
                                return 1;
                            })))
            );

        });
    }

    // 🚨 CHECK WORDS
    private boolean isBanned(String msg) {
        for (String w : bannedWords) {
            if (msg.contains(w)) {
                return true;
            }
        }
        return false;
    }

    // ⚡ PUNISH SYSTEM
    private void punish(ServerPlayerEntity p, String msg) {

        UUID id = p.getUuid();

        int warn = warnings.getOrDefault(id, 0) + 1;
        warnings.put(id, warn);

        p.sendMessage(Text.literal("⚠ Message blocked by Dali Security"), false);

        if (warn == 1) {
            p.sendMessage(Text.literal("⚠ Warning 1/3"), false);
        }

        if (warn == 2) {
            p.sendMessage(Text.literal("⚠ Warning 2/3 - Behavior detected"), false);
        }

        if (warn >= 3) {
            locked.put(id, true);
            p.networkHandler.disconnect(Text.literal("🔒 You were removed by Dali Security System"));
        }
    }
}