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

    private static boolean systemOn = false;
    private static UUID owner = null;

    private static final String KEY = "DALI";

    private static final Set<UUID> admins = new HashSet<>();
    private static final Map<UUID, Integer> warnings = new HashMap<>();

    @Override
    public void onInitialize() {

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

                                log("OWNER SET: " + p.getName().getString());

                                ctx.getSource().sendFeedback(
                                        () -> Text.literal("§aYOU ARE OWNER (GOD MODE ACTIVE)"),
                                        false
                                );
                            } else {
                                ctx.getSource().sendFeedback(
                                        () -> Text.literal("§4WRONG KEY"),
                                        false
                                );
                            }

                            return 1;
                        }))
            );

            // ⚙️ MAIN CONTROL
            dispatcher.register(
                CommandManager.literal("daliscript")
                    .executes(ctx -> {

                        ServerPlayerEntity p = ctx.getSource().getPlayer();

                        if (!isOwner(p) && !isAdmin(p)) {
                            ctx.getSource().sendFeedback(() ->
                                    Text.literal("§cNO PERMISSION"), false);
                            return 0;
                        }

                        systemOn = !systemOn;

                        ctx.getSource().sendFeedback(() ->
                                Text.literal(systemOn ? "§aSYSTEM ONLINE" : "§cSYSTEM OFFLINE"),
                                false
                        );

                        return 1;
                    })
            );

            // 🎮 GAMEMODE SYSTEM
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

                                log("Gamemode changed: " + gm.name());

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

                                    if (!isOwner(ctx.getSource().getPlayer())) return 0;

                                    String name = StringArgumentType.getString(ctx, "player");
                                    ServerPlayerEntity target = ctx.getSource().getServer()
                                            .getPlayerManager()
                                            .getPlayer(name);

                                    if (target != null) {
                                        admins.add(target.getUuid());
                                        log("ADMIN ADDED: " + name);
                                    }

                                    return 1;
                                })))
                    )
            );

        });

        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    // 💀 CORE SYSTEM
    private void tick(MinecraftServer server) {

        if (!systemOn) return;

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {

            // 💀 Hardcore rules
            server.getCommandManager().executeWithPrefix(server.getCommandSource(),
                    "difficulty hard");

            server.getCommandManager().executeWithPrefix(server.getCommandSource(),
                    "gamerule keepInventory false");

            // 🛡 Anti-cheat
            if (!isOwner(p) && !isAdmin(p)) {

                if (p.getAbilities().flying) {
                    p.getAbilities().flying = false;
                    warn(p, "FLY DETECTED");
                }

                if (p.getVelocity().length() > 1.2) {
                    p.setVelocity(0, 0, 0);
                    warn(p, "SPEED HACK");
                }

                if (p.isCreative()) {
                    p.changeGameMode(GameMode.SURVIVAL);
                    warn(p, "CREATIVE BLOCKED");
                }
            }

            // 📊 Ban system
            int w = warnings.getOrDefault(p.getUuid(), 0);

            if (w >= 5 && !isOwner(p)) {
                p.networkHandler.disconnect(Text.literal("BANNED BY DaliScript V8"));
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

    // ⚠️ WARN SYSTEM
    private void warn(ServerPlayerEntity p, String reason) {
        UUID id = p.getUuid();
        int w = warnings.getOrDefault(id, 0) + 1;
        warnings.put(id, w);

        p.sendMessage(Text.literal("§c[WARN] " + reason + " (" + w + "/5)"), false);
        log(p.getName().getString() + " -> " + reason);
    }

    // 📡 LOG SYSTEM
    private void log(String msg) {
        System.out.println("[DaliScript V8] " + msg);
    }
}