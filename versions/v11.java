package com.dali.daliscript;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
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
    private static UUID owner = null;

    private static final String KEY = "DALI";

    private static final Set<UUID> admins = new HashSet<>();
    private static final Map<UUID, Integer> warnings = new HashMap<>();

    private static File dataFile;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // 💾 SAVE STRUCT
    static class Data {
        boolean systemOn;
        String owner;
        List<String> admins;
    }

    @Override
    public void onInitialize() {

        // 💾 LOAD DATA
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                File dir = new File("config/daliscript");
                dir.mkdirs();

                dataFile = new File(dir, "data.json");

                if (dataFile.exists()) {
                    Data d = gson.fromJson(new FileReader(dataFile), Data.class);

                    systemOn = d.systemOn;
                    if (d.owner != null) owner = UUID.fromString(d.owner);

                    if (d.admins != null) {
                        for (String a : d.admins) {
                            admins.add(UUID.fromString(a));
                        }
                    }
                }

                log("DATA LOADED");

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // 💾 SAVE ON STOP
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> save());

        // ⚙️ COMMANDS
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // 🔑 KEY SYSTEM
            dispatcher.register(
                CommandManager.literal("daliscriptkey")
                    .then(CommandManager.argument("key", StringArgumentType.string())
                        .executes(ctx -> {

                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            if (p == null) return 0;

                            String input = StringArgumentType.getString(ctx, "key");

                            if (input.equals(KEY)) {
                                owner = p.getUuid();
                                systemOn = true;

                                send(p, "§aOWNER GRANTED (V12 GOD MODE)");
                                save();
                                log("OWNER SET");
                            } else {
                                send(p, "§4WRONG KEY");
                            }

                            return 1;
                        }))
            );

            // ⚙️ TOGGLE
            dispatcher.register(
                CommandManager.literal("daliscript")
                    .executes(ctx -> {

                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        if (!isOwnerOrAdmin(p)) return 0;

                        systemOn = !systemOn;
                        send(p, systemOn ? "§aSYSTEM ON V12" : "§cSYSTEM OFF");

                        save();
                        return 1;
                    })
            );

            // 🎮 GAMEMODE
            dispatcher.register(
                CommandManager.literal("daliscript")
                    .then(CommandManager.literal("gamemode")
                        .then(CommandManager.argument("mode", IntegerArgumentType.integer(0, 3))
                            .executes(ctx -> {

                                ServerPlayerEntity p = ctx.getSource().getPlayer();
                                if (p == null || !systemOn) return 0;

                                int mode = IntegerArgumentType.getInteger(ctx, "mode");

                                GameMode gm = switch (mode) {
                                    case 0 -> GameMode.SURVIVAL;
                                    case 1 -> GameMode.CREATIVE;
                                    case 2 -> GameMode.ADVENTURE;
                                    case 3 -> GameMode.SPECTATOR;
                                    default -> GameMode.SURVIVAL;
                                };

                                p.changeGameMode(gm);
                                send(p, "§aMODE: " + gm.name());

                                return 1;
                            }))
                    )
            );
        });

        // 💀 ENGINE
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    // 💀 CORE ENGINE
    private void tick(MinecraftServer server) {

        if (!systemOn) return;

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {

            // 💀 HARDCORE
            server.getCommandManager().executeWithPrefix(server.getCommandSource(),
                    "difficulty hard");

            server.getCommandManager().executeWithPrefix(server.getCommandSource(),
                    "gamerule keepInventory false");

            // 🛡 ANTI CHEAT
            if (!isOwner(p) && !isAdmin(p)) {

                if (p.getAbilities().flying) {
                    p.getAbilities().flying = false;
                    warn(p, "FLY DETECTED");
                }

                if (p.getVelocity().length() > 1.25) {
                    p.setVelocity(0, 0, 0);
                    warn(p, "SPEED DETECTED");
                }

                if (p.isCreative()) {
                    p.changeGameMode(GameMode.SURVIVAL);
                    warn(p, "CREATIVE BLOCKED");
                }
            }

            // 💀 BAN
            int w = warnings.getOrDefault(p.getUuid(), 0);

            if (w >= 5) {
                p.networkHandler.disconnect(Text.literal("BANNED BY DaliScript V12"));
                log("BAN: " + p.getName().getString());
            }
        }
    }

    // 🔐 ROLES
    private boolean isOwner(ServerPlayerEntity p) {
        return p != null && owner != null && p.getUuid().equals(owner);
    }

    private boolean isAdmin(ServerPlayerEntity p) {
        return p != null && admins.contains(p.getUuid());
    }

    private boolean isOwnerOrAdmin(ServerPlayerEntity p) {
        return isOwner(p) || isAdmin(p);
    }

    // 💬 SEND
    private void send(ServerPlayerEntity p, String msg) {
        if (p != null) p.sendMessage(Text.literal(msg), false);
    }

    // ⚠️ WARN
    private void warn(ServerPlayerEntity p, String reason) {
        UUID id = p.getUuid();
        int w = warnings.getOrDefault(id, 0) + 1;
        warnings.put(id, w);

        send(p, "§c[WARN] " + reason + " (" + w + "/5)");
        log(p.getName().getString() + " -> " + reason);
    }

    // 💾 SAVE SYSTEM
    private void save() {
        try {
            Data d = new Data();
            d.systemOn = systemOn;
            d.owner = owner != null ? owner.toString() : null;
            d.admins = new ArrayList<>();

            for (UUID u : admins) {
                d.admins.add(u.toString());
            }

            FileWriter fw = new FileWriter(dataFile);
            gson.toJson(d, fw);
            fw.close();

            log("DATA SAVED");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 📡 LOG
    private void log(String msg) {
        System.out.println("[DaliScript V12] " + msg);
    }
}