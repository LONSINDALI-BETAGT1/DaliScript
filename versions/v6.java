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

    private static boolean enabled = false;
    private static boolean locked = true;
    private static UUID owner = null;

    private static final String KEY = "DALI";

    private static final Map<UUID, Integer> warnings = new HashMap<>();
    private static final List<String> logs = new ArrayList<>();

    @Override
    public void onInitialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // 🔑 تعيين المالك
            dispatcher.register(
                CommandManager.literal("daliscriptkey")
                    .then(CommandManager.argument("key", StringArgumentType.string())
                        .executes(ctx -> {

                            String input = StringArgumentType.getString(ctx, "key");
                            ServerPlayerEntity player = ctx.getSource().getPlayer();

                            if (player == null) return 0;

                            if (input.equals(KEY)) {
                                locked = false;
                                owner = player.getUuid();

                                log("OWNER SET: " + player.getName().getString());

                                ctx.getSource().sendFeedback(() ->
                                        Text.literal("§aYOU ARE NOW OWNER"), false);
                            } else {
                                locked = true;
                                owner = null;

                                ctx.getSource().sendFeedback(() ->
                                        Text.literal("§4WRONG KEY - SYSTEM LOCKED"), false);
                            }

                            return 1;
                        }))
            );

            // ⚙️ تشغيل / إيقاف
            dispatcher.register(
                CommandManager.literal("daliscript")
                    .executes(ctx -> {

                        if (!isOwner(ctx.getSource().getPlayer())) {
                            ctx.getSource().sendFeedback(() ->
                                    Text.literal("§cNO PERMISSION"), false);
                            return 0;
                        }

                        enabled = !enabled;
                        log("SYSTEM " + (enabled ? "ON" : "OFF"));

                        ctx.getSource().sendFeedback(() ->
                                Text.literal(enabled ? "§aSYSTEM ONLINE" : "§cSYSTEM OFFLINE"), false);

                        return 1;
                    })
            );

            // 🎮 Gamemode Control
            dispatcher.register(
                CommandManager.literal("daliscript")
                    .then(CommandManager.literal("gamemode")
                        .then(CommandManager.argument("mode", IntegerArgumentType.integer(0, 3))
                            .executes(ctx -> {

                                if (!isOwner(ctx.getSource().getPlayer()) || !enabled) {
                                    return 0;
                                }

                                int mode = IntegerArgumentType.getInteger(ctx, "mode");
                                ServerPlayerEntity p = ctx.getSource().getPlayer();

                                if (p == null) return 0;

                                GameMode gm = switch (mode) {
                                    case 0 -> GameMode.SURVIVAL;
                                    case 1 -> GameMode.CREATIVE;
                                    case 2 -> GameMode.ADVENTURE;
                                    case 3 -> GameMode.SPECTATOR;
                                    default -> GameMode.SURVIVAL;
                                };

                                p.changeGameMode(gm);
                                log("GAMEMODE SET: " + gm.name());

                                return 1;
                            }))
                    )
            );

        });

        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    private void tick(MinecraftServer server) {

        if (!enabled) return;

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {

            // 💀 Hardcore enforcement
            server.getCommandManager().executeWithPrefix(server.getCommandSource(),
                    "difficulty hard");

            server.getCommandManager().executeWithPrefix(server.getCommandSource(),
                    "gamerule keepInventory false");

            // 🛡 Anti-cheat core

            if (p.getAbilities().flying && !isOwner(p)) {
                p.getAbilities().flying = false;
                warn(p, "FLY DETECTED");
            }

            if (p.getVelocity().length() > 1.25) {
                p.setVelocity(0, 0, 0);
                warn(p, "SPEED HACK");
            }

            if (p.isCreative() && !isOwner(p)) {
                p.changeGameMode(GameMode.SURVIVAL);
                warn(p, "CREATIVE BLOCKED");
            }

            // 📊 Kick system
            int w = warnings.getOrDefault(p.getUuid(), 0);

            if (w >= 5 && !isOwner(p)) {
                p.networkHandler.disconnect(Text.literal("BANNED BY DaliScript V7"));
                log("PLAYER BANNED: " + p.getName().getString());
            }
        }
    }

    private boolean isOwner(ServerPlayerEntity p) {
        return p != null && owner != null && p.getUuid().equals(owner);
    }

    private void warn(ServerPlayerEntity p, String reason) {
        UUID id = p.getUuid();
        int w = warnings.getOrDefault(id, 0) + 1;
        warnings.put(id, w);

        p.sendMessage(Text.literal("§c[WARN] " + reason + " (" + w + "/5)"), false);

        log(p.getName().getString() + " -> " + reason);
    }

    private void log(String msg) {
        logs.add(msg);
        System.out.println("[DaliScript] " + msg);
    }
}