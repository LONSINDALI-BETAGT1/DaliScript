package com.dali.daliscript;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;

public class DaliScriptMod implements ModInitializer {

    // 🚨 BEHAVIOR SCORE (كل لاعب)
    private static final Map<UUID, Integer> behavior = new HashMap<>();

    // 📍 LAST POSITION TRACKING
    private static final Map<UUID, Double> lastX = new HashMap<>();
    private static final Map<UUID, Double> lastZ = new HashMap<>();

    @Override
    public void onInitialize() {

        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    // 🧠 MAIN SECURITY ENGINE
    private void tick(MinecraftServer server) {

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {

            UUID id = p.getUuid();

            double x = p.getX();
            double z = p.getZ();

            // 📍 INIT
            if (!lastX.containsKey(id)) {
                lastX.put(id, x);
                lastZ.put(id, z);
                behavior.put(id, 0);
                continue;
            }

            double lx = lastX.get(id);
            double lz = lastZ.get(id);

            double distance = Math.sqrt((x - lx) * (x - lx) + (z - lz) * (z - lz));

            int score = behavior.getOrDefault(id, 0);

            // ⚡ SPEED CHECK (SIMULATION)
            if (distance > 1.2) {
                score -= 1;
                p.sendMessage(Text.literal("⚠ DaliGuard: Suspicious movement detected"), false);
            }

            // 🧱 FALLBACK NORMAL
            else {
                score += 1;
            }

            // 📊 UPDATE SCORE
            behavior.put(id, score);

            // 🚨 LOW SCORE ACTION
            if (score < -10) {
                p.networkHandler.disconnect(Text.literal("🔒 Kicked by Dali Security (Anti-Cheat Triggered)"));
            }

            // 🔄 UPDATE POSITION
            lastX.put(id, x);
            lastZ.put(id, z);
        }
    }
}