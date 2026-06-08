package com.dali.daliscript;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class DaliScriptMod implements ModInitializer {

    // 🔥 CORE STATE
    private static boolean systemOn = false;
    private static boolean cheatMode = false;

    private static UUID owner = null;
    private static final String KEY = "DALI";

    private static final Set<UUID> admins = new HashSet<>();
    private static final Map<UUID, Integer> warnings = new HashMap<>();

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static File dataFile;
    private static File auditFile;

    // 💾 DATA MODEL
    static class Data {
        boolean systemOn;
        boolean cheatMode;
        String owner;
        List<String> admins;
    }

    @Override
    public void onInitialize() {

        // 💾 INIT FILES
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                File dir = new File("config/daliscript");
                dir.mkdirs();

                dataFile = new File(dir, "data.json");
                auditFile = new File(dir, "audit.log");

                load();

                log("V16 LOADED");

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> save());

        // ⚙️ COMMANDS
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("daliscriptkey")
                    .then(net.minecraft.server.command.CommandManager.literal("DALI")
                        .executes(ctx -> {

                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            if (p == null) return 0;

                            owner = p.getUuid();
                            systemOn = true;

                            send(p, "§aOWNER MODE ACTIVE (V16)");

                            audit("OWNER SET: " + p.getName().getString());
                            save();

                            return 1;
                        }))
            );

            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("daliscript")
                    .executes(ctx -> {

                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        if (!isOwner(p)) return 0;

                        systemOn = !systemOn;
                        send(p, systemOn ? "§aSYSTEM ON V16" : "§cSYSTEM OFF");

                        audit("SYSTEM TOGGLED");
                        save();

                        return 1;
                    })
            );

        });

        // 💀 ENGINE
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    // 💀 CORE ENGINE
    private void tick(MinecraftServer server) {

        if (!systemOn) return;

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {

            // 💀 HARDCORE RULES
            server.getCommandManager().executeWithPrefix(server.getCommandSource(),
                    "difficulty hard");

            server.getCommandManager().executeWithPrefix(server.getCommandSource(),
                    "gamerule keepInventory false");

            // 🛡 ANTI CHEAT
            if (!isOwner(p) && !cheatMode) {

                if (p.getAbilities().flying) {
                    p.getAbilities().flying = false;
                    warn(p, "FLY DETECTED");
                }

                if (p.isCreative()) {
                    p.changeGameMode(GameMode.SURVIVAL);
                    warn(p, "CREATIVE BLOCKED");
                }

                if (p.getVelocity().length() > 1.2) {
                    p.setVelocity(0, 0, 0);
                    warn(p, "SPEED DETECTED");
                }
            }

            // 💀 BAN
            if (warnings.getOrDefault(p.getUuid(), 0) >= 5) {
                p.networkHandler.disconnect(Text.literal("BANNED BY DaliScript V16"));
                audit("BAN: " + p.getName().getString());
            }
        }
    }

    // 🔐 ROLES
    private boolean isOwner(ServerPlayerEntity p) {
        return p != null && owner != null && p.getUuid().equals(owner);
    }

    // ⚠️ WARN
    private void warn(ServerPlayerEntity p, String reason) {
        UUID id = p.getUuid();
        int w = warnings.getOrDefault(id, 0) + 1;
        warnings.put(id, w);

        send(p, "§c[WARN] " + reason + " (" + w + "/5)");
        audit(p.getName().getString() + " -> " + reason);
    }

    // 💬 SEND
    private void send(ServerPlayerEntity p, String msg) {
        if (p != null) p.sendMessage(Text.literal(msg), false);
    }

    // 📜 AUDIT LOG
    private void audit(String msg) {
        try {
            FileWriter fw = new FileWriter(auditFile, true);
            fw.write(msg + "\n");
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("[DaliScript V16] " + msg);
    }

    // 💾 SAVE
    private void save() {
        try {
            Data d = new Data();
            d.systemOn = systemOn;
            d.cheatMode = cheatMode;
            d.owner = owner != null ? owner.toString() : null;
            d.admins = new ArrayList<>();

            FileWriter fw = new FileWriter(dataFile);
            gson.toJson(d, fw);
            fw.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 💾 LOAD
    private void load() {
        try {
            if (dataFile == null || !dataFile.exists()) return;

            Data d = gson.fromJson(new FileReader(dataFile), Data.class);

            systemOn = d.systemOn;
            cheatMode = d.cheatMode;

            if (d.owner != null)
                owner = UUID.fromString(d.owner);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 📡 LOG
    private void log(String msg) {
        System.out.println("[DaliScript V16] " + msg);
    }
}