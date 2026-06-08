package com.dali.daliscript;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;

public class DaliScriptMod implements ModInitializer {

    // 💀 CORE SYSTEM
    private static boolean systemOn = true;
    private static final String OWNER_KEY = "DALI";
    private static final String COUNTRY_CODE = "+2699";

    private static UUID owner;

    private static final Set<UUID> frozen = new HashSet<>();
    private static final Map<UUID, Integer> warnings = new HashMap<>();

    @Override
    public void onInitialize() {

        // ⚙️ COMMANDS
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // 🔑 OWNER KEY
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("daliscriptkey")
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        if (p == null) return 0;

                        owner = p.getUuid();
                        send(p, "§aDaliScript V17 OWNER ACTIVATED " + COUNTRY_CODE);
                        return 1;
                    })
            );

            // 📱 DALIPHONE OPEN
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("daliphone")
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        send(p,
                                "§b📱 DaliPhone MENU\n" +
                                "§7/dalicord\n" +
                                "§7/dalimessage\n" +
                                "§7/rules\n" +
                                "§7/info\n" +
                                "§7/shop"
                        );
                        return 1;
                    })
            );

            // 💬 DALICORD
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("dalicord")
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        send(p, "§b[Dalicord] System chat active");
                        return 1;
                    })
            );

            // 📜 RULES
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("rules")
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        send(p,
                                "§cRULES:\n" +
                                "- No hacks\n" +
                                "- No abuse\n" +
                                "- Owner controls system"
                        );
                        return 1;
                    })
            );

            // ℹ️ INFO
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("info")
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        send(p,
                                "§aDaliScript V17\n" +
                                "System: " + systemOn + "\n" +
                                "Owner: " + (owner != null)
                        );
                        return 1;
                    })
            );

        });

        // 💀 ENGINE
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    // 💀 MAIN ENGINE
    private void tick(MinecraftServer server) {

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {

            // 🧠 RULE ENFORCEMENT
            if (systemOn) {

                server.getCommandManager().executeWithPrefix(
                        server.getCommandSource(),
                        "difficulty hard"
                );

                // 💀 FREEZE SYSTEM
                if (frozen.contains(p.getUuid())) {
                    p.setVelocity(0, 0, 0);
                }

                // 🛡 SIMPLE CHEAT DETECTION
                if (!isOwner(p)) {

                    if (p.getAbilities().flying) {
                        freeze(p);
                        warn(p, "FLY DETECTED");
                    }

                    if (p.getVelocity().length() > 1.3) {
                        freeze(p);
                        warn(p, "SPEED ANOMALY");
                    }
                }
            }
        }
    }

    // ❄️ FREEZE SYSTEM
    private void freeze(ServerPlayerEntity p) {
        frozen.add(p.getUuid());
        send(p, "§cYOU ARE FROZEN FOR SAFETY");
    }

    // ⚠️ WARN
    private void warn(ServerPlayerEntity p, String reason) {
        UUID id = p.getUuid();
        int w = warnings.getOrDefault(id, 0) + 1;
        warnings.put(id, w);

        send(p, "§c[WARN] " + reason + " (" + w + "/5)");

        if (w >= 5) {
            p.networkHandler.disconnect(Text.literal("KICKED BY DaliScript V17"));
        }
    }

    // 👑 OWNER CHECK
    private boolean isOwner(ServerPlayerEntity p) {
        return p != null && owner != null && p.getUuid().equals(owner);
    }

    // 💬 SEND
    private void send(ServerPlayerEntity p, String msg) {
        if (p != null) p.sendMessage(Text.literal(msg), false);
    }
}