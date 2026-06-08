package com.dali.daliscript;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;

public class DaliScriptMod implements ModInitializer {

    // 📊 PLAYER PROFILE
    private static final Map<UUID, Integer> score = new HashMap<>();
    private static final Map<UUID, Integer> violations = new HashMap<>();
    private static final Map<UUID, String> status = new HashMap<>();

    // 📍 POSITION TRACKING
    private static final Map<UUID, Double> lastX = new HashMap<>();
    private static final Map<UUID, Double> lastZ = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    // 🧠 SENTINEL ENGINE
    private void tick(MinecraftServer server) {

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {

            UUID id = p.getUuid();

            double x = p.getX();
            double z = p.getZ();

            initPlayer(id);

            double lx = lastX.get(id);
            double lz = lastZ.get(id);

            double distance = Math.sqrt((x - lx) * (x - lx) + (z - lz) * (z - lz));

            int sc = score.get(id);

            // ⚡ MOVEMENT ANALYSIS
            if (distance > 1.3) {
                sc -= 2;
                addViolation(id, "Speed anomaly detected");
                send(p, "⚠ Sentinel: Movement suspicious");
            } else {
                sc += 1;
            }

            score.put(id, sc);
            lastX.put(id, x);
            lastZ.put(id, z);

            // 🔍 DECISION ENGINE
            evaluatePlayer(p, id, sc);
        }
    }

    // 🧠 INIT
    private void initPlayer(UUID id) {
        score.putIfAbsent(id, 0);
        violations.putIfAbsent(id, 0);
        status.putIfAbsent(id, "NORMAL");
        lastX.putIfAbsent(id, 0.0);
        lastZ.putIfAbsent(id, 0.0);
    }

    // 📊 VIOLATION SYSTEM
    private void addViolation(UUID id, String reason) {
        violations.put(id, violations.getOrDefault(id, 0) + 1);
    }

    // 🧠 AI DECISION ENGINE
    private void evaluatePlayer(ServerPlayerEntity p, UUID id, int sc) {

        int v = violations.get(id);

        if (v >= 5 || sc < -15) {

            status.put(id, "LOCKED");

            p.networkHandler.disconnect(
                    Text.literal("🔒 Dali Sentinel: Account Locked (High Risk Detected)")
            );
        }

        else if (v >= 3) {

            status.put(id, "SUSPICIOUS");
            send(p, "⚠ Sentinel: You are under observation");
        }

        else {
            status.put(id, "NORMAL");
        }
    }

    // 💬 SEND
    private void send(ServerPlayerEntity p, String msg) {
        if (p != null) {
            p.sendMessage(Text.literal(msg), false);
        }
    }
}