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

    // 💀 CORE
    private static boolean systemOn = false;
    private static UUID owner = null;

    private static final String KEY = "DALI";

    private static final Set<UUID> admins = new HashSet<>();
    private static final Map<UUID, Integer> warnings = new HashMap<>();

    @Override
    public void onInitialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // 🔑 KEY
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

                                send(p, "§aOWNER MODE ACTIVE (V14)");
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
                        if (!isOwner(p)) return 0;

                        systemOn = !systemOn;
                        send(p, systemOn ? "§aSYSTEM ON" : "§cSYSTEM OFF");

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

            // 🔥 CHEAT COMMANDS
            dispatcher.register(
                CommandManager.literal("daliscript")
                    .then(CommandManager.literal("cheat")
                        .then(CommandManager.literal("fly")
                            .executes(ctx -> {
                                ServerPlayerEntity p = ctx.getSource().getPlayer();
                                if (!isOwner(p) || !systemOn) return 0;

                                p.getAbilities().allowFlying = true;
                                p.getAbilities().flying = true;
                                send(p, "§bFLY ENABLED (CHEAT MODE)");
                                return 1;
                            }))
                        .then(CommandManager.literal("heal")
                            .executes(ctx -> {
                                ServerPlayerEntity p = ctx.getSource().getPlayer();
                                if (!isOwner(p)) return 0;

                                p.setHealth(20.0f);
                                send(p, "§aHEALED");
                                return 1;
                            }))
                        .then(CommandManager.literal("speed")
                            .executes(ctx -> {
                                ServerPlayerEntity p = ctx.getSource().getPlayer();
                                if (!isOwner(p)) return 0;

                                p.getAbilities().setWalkSpeed(0.3f);
                                send(p, "§eSPEED BOOST");
                                return 1;
                            }))
                    )
            );

        });

        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    // 💀 HARDCORE ENGINE
    private void tick(MinecraftServer server) {

        if (!systemOn) return;

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {

            server.getCommandManager().executeWithPrefix(server.getCommandSource(),
                    "difficulty hard");

            server.getCommandManager().executeWithPrefix(server.getCommandSource(),
                    "gamerule keepInventory false");

            // 🛡 Anti cheat للغير OWNER
            if (!isOwner(p)) {

                if (p.getAbilities().flying) {
                    p.getAbilities().flying = false;
                    warn(p, "FLY BLOCKED");
                }

                if (p.isCreative()) {
                    p.changeGameMode(GameMode.SURVIVAL);
                    warn(p, "CREATIVE BLOCKED");
                }
            }

            // 💀 HARDCORE DEATH SYSTEM
            int w = warnings.getOrDefault(p.getUuid(), 0);

            if (w >= 5 && !isOwner(p)) {
                p.networkHandler.disconnect(Text.literal("BANNED (V14 HARDCORE)"));
            }
        }
    }

    // 🔐 OWNER CHECK
    private boolean isOwner(ServerPlayerEntity p) {
        return p != null && owner != null && p.getUuid().equals(owner);
    }

    // ⚠️ WARN
    private void warn(ServerPlayerEntity p, String reason) {
        UUID id = p.getUuid();
        warnings.put(id, warnings.getOrDefault(id, 0) + 1);

        send(p, "§c[WARN] " + reason);
    }

    // 💬 SEND
    private void send(ServerPlayerEntity p, String msg) {
        if (p != null) p.sendMessage(Text.literal(msg), false);
    }
}