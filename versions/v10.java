package com.dali.daliscript;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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
import java.io.FileWriter;
import java.util.*;

public class DaliScriptMod implements ModInitializer {

    // 🔥 CORE STATE (will be saved)
    private static boolean systemOn = false;
    private static UUID owner = null;

    private static final String KEY = "DALI";

    private static final Set<UUID> admins = new HashSet<>();
    private static final Map<UUID, Integer> warnings = new HashMap<>();

    private static File logFile;

    @Override
    public void onInitialize() {

        // 📁 CREATE LOG FILE
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                File dir = new File("config/daliscript");
                dir.mkdirs();

                logFile = new File(dir, "logs.txt");
                log("SERVER STARTED");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // ⚙️ COMMANDS
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // 🔑 KEY SYSTEM
            dispatcher.register(
                CommandManager.literal("daliscriptkey")
                    .then(CommandManager.argument("key", StringArgumentType.string())
                        .executes(ctx -> {

                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            String input = StringArgumentType.getString(ctx, "key");

                            if (p == null) return 0;

                            if (input.equals(KEY)) {
                                owner = p.getUuid();
                                systemOn = true;

                                send(p, "§aOWNER GRANTED (V11)");
                                log("OWNER: " + p.getName().getString());
                            } else {
                                send(p, "§4ACCESS DENIED");
                            }

                            return 1;
                        }))
            );

            // ⚙️ MAIN TOGGLE
            dispatcher.register(
                CommandManager.literal("daliscript")
                    .executes(ctx -> {

                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        if (!isOwnerOrAdmin(p)) {
                            send(p, "§cNO PERMISSION");
                            return 0;
                        }

                        systemOn = !systemOn;
                        send(p, systemOn ? "§aSYSTEM ON (V11)" : "§cSYSTEM OFF");

                        log("SYSTEM TOGGLED: " + systemOn);

                        return 1;
                    })
            );

            // 🎮 GAMEMODE CONTROL
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

        // 💀 ENGINE LOOP
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

            // 🛡 ANTI CHEAT V11
            if (!isOwner(p) && !isAdmin(p)) {

                if (p.getAbilities().flying) {
                    p.getAbilities().flying = false;
                    warn(p, "FLY DETECTED");
                }

                if (p.getVelocity().length() > 1.2) {
                    p.setVelocity(0, 0, 0);
                    warn(p, "SPEED DETECTED");
                }

                if (p.isCreative()) {
                    p.changeGameMode(GameMode.SURVIVAL);
                    warn(p, "CREATIVE BLOCKED");
                }
            }

            // 💀 BAN SYSTEM
            int w = warnings.getOrDefault(p.getUuid(), 0);

            if (w >= 5) {
                p.networkHandler.disconnect(Text.literal("BANNED BY DaliScript V11"));
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

    // 📡 FILE LOGGING
    private void log(String msg) {
        try {
            if (logFile != null) {
                FileWriter fw = new FileWriter(logFile, true);
                fw.write(msg + "\n");
                fw.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("[DaliScript V11] " + msg);
    }
}