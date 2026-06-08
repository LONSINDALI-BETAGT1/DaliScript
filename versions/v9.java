package com.dali.daliscript;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.util.*;

public class DaliScriptMod implements ModInitializer {

    // 🔥 CORE STATE
    private static boolean systemOn = false;
    private static UUID owner = null;

    private static final String KEY = "DALI";

    private static final Set<UUID> admins = new HashSet<>();
    private static final Map<UUID, Integer> warnings = new HashMap<>();

    // 💾 PERSISTENCE (بسيط داخل RAM + قابل للتطوير إلى ملفات)
    private static boolean hardcoreLock = true;

    @Override
    public void onInitialize() {

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
                                hardcoreLock = false;

                                send(p, "§aOWNER SET — GOD MODE ACTIVE");
                                log("OWNER: " + p.getName().getString());
                            } else {
                                send(p, "§4WRONG KEY — SYSTEM LOCKED");
                                systemOn = false;
                            }

                            return 1;
                        }))
            );

            // ⚙️ MAIN CONTROL
            dispatcher.register(
                CommandManager.literal("daliscript")
                    .executes(ctx -> {

                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        if (!isOwnerOrAdmin(p)) {
                            send(p, "§cNO PERMISSION");
                            return 0;
                        }

                        systemOn = !systemOn;
                        send(p, systemOn ? "§aSYSTEM ON" : "§cSYSTEM OFF");

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
                                send(p, "§aGAMEMODE: " + gm.name());

                                return 1;
                            }))
                    )
            );

            // 👑 ADMIN SYSTEM
            dispatcher.register(
                CommandManager.literal("daliscript")
                    .then(CommandManager.literal("admin")
                        .then(CommandManager.literal("add")
                            .then(CommandManager.argument("player", StringArgumentType.string())
                                .executes(ctx -> {

                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    if (!isOwner(p)) return 0;

                                    String name = StringArgumentType.getString(ctx, "player");

                                    ServerPlayerEntity target =
                                            ctx.getSource().getServer()
                                                    .getPlayerManager()
                                                    .getPlayer(name);

                                    if (target != null) {
                                        admins.add(target.getUuid());
                                        send(p, "§aADMIN ADDED: " + name);
                                        log("ADMIN: " + name);
                                    }

                                    return 1;
                                })))
                    )
            );
        });

        // 💀 CORE ENGINE
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    // 💀 MAIN SYSTEM LOOP
    private void tick(MinecraftServer server) {

        if (!systemOn) return;

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {

            // 💀 HARDCORE RULES
            server.getCommandManager().executeWithPrefix(server.getCommandSource(),
                    "difficulty hard");

            server.getCommandManager().executeWithPrefix(server.getCommandSource(),
                    "gamerule keepInventory false");

            // 🛡 ANTI CHEAT
            if (!isOwner(p) && !isAdmin(p)) {

                // Fly detect
                if (p.getAbilities().flying) {
                    p.getAbilities().flying = false;
                    warn(p, "FLY DETECTED");
                }

                // Speed detect
                if (p.getVelocity().length() > 1.3) {
                    p.setVelocity(0, 0, 0);
                    warn(p, "SPEED HACK");
                }

                // Creative block
                if (p.isCreative()) {
                    p.changeGameMode(GameMode.SURVIVAL);
                    warn(p, "CREATIVE BLOCKED");
                }
            }

            // 💀 BAN SYSTEM
            int w = warnings.getOrDefault(p.getUuid(), 0);

            if (w >= 5 && !isOwner(p)) {
                p.networkHandler.disconnect(Text.literal("BANNED BY DaliScript V10"));
                log("BANNED: " + p.getName().getString());
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

    // ⚠️ WARN SYSTEM
    private void warn(ServerPlayerEntity p, String reason) {
        UUID id = p.getUuid();
        int w = warnings.getOrDefault(id, 0) + 1;
        warnings.put(id, w);

        send(p, "§c[WARN] " + reason + " (" + w + "/5)");
        log(p.getName().getString() + " -> " + reason);
    }

    // 💬 CHAT
    private void send(ServerPlayerEntity p, String msg) {
        if (p != null) p.sendMessage(Text.literal(msg), false);
    }

    // 📡 LOGGING
    private void log(String msg) {
        System.out.println("[DaliScript V10] " + msg);
    }
}