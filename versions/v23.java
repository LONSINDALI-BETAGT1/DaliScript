package com.dali.daliscript;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;

public class DaliScriptMod implements ModInitializer {

    // 💎 PREMIUM DATA
    private static class PremiumData {
        long startTime;
        boolean active;
    }

    // UUID -> Premium
    private static final Map<UUID, PremiumData> premium = new HashMap<>();

    // 💎 PRICE SIMULATION
    private static final int PREMIUM_COST_STACKS = 36;

    @Override
    public void onInitialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // 💎 BUY PREMIUM
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("daliscript")
                    .then(net.minecraft.server.command.CommandManager.literal("premium")
                        .then(net.minecraft.server.command.CommandManager.literal("buy")
                            .executes(ctx -> {

                                ServerPlayerEntity p = ctx.getSource().getPlayer();

                                PremiumData data = new PremiumData();
                                data.startTime = System.currentTimeMillis();
                                data.active = true;

                                premium.put(p.getUuid(), data);

                                send(p,
                                    "§a💎 PREMIUM ACTIVATED\n" +
                                    "Cost: 36 Stacks Emeralds\n" +
                                    "Duration: 30 days (server time)"
                                );

                                return 1;
                            })))
            );

            // 📊 STATUS
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("daliscript")
                    .then(net.minecraft.server.command.CommandManager.literal("premium")
                        .then(net.minecraft.server.command.CommandManager.literal("status")
                            .executes(ctx -> {

                                ServerPlayerEntity p = ctx.getSource().getPlayer();

                                PremiumData d = premium.get(p.getUuid());

                                if (d == null || !d.active) {
                                    send(p, "§c❌ No Premium Active");
                                    return 1;
                                }

                                long days = (System.currentTimeMillis() - d.startTime) / (1000L * 60 * 60 * 24);

                                send(p,
                                    "§6💎 PREMIUM ACTIVE\n" +
                                    "Days used: " + days + "/30"
                                );

                                return 1;
                            })))
            );

        });

        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    // 💀 CHECK EXPIRY SYSTEM
    private void tick(MinecraftServer server) {

        long now = System.currentTimeMillis();

        for (UUID id : new ArrayList<>(premium.keySet())) {

            PremiumData d = premium.get(id);

            if (d == null || !d.active) continue;

            long days = (now - d.startTime) / (1000L * 60 * 60 * 24);

            if (days >= 30) {
                d.active = false;
                premium.put(id, d);
            }
        }

        // 🧠 APPLY PREMIUM FEATURES
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {

            PremiumData d = premium.get(p.getUuid());

            if (d != null && d.active) {

                // 💎 Premium perks (visual only)
                p.sendMessage(Text.literal("§6💎 Premium Active"), true);
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