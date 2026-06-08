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

import java.util.HashMap;
import java.util.UUID;

public class DaliScriptMod implements ModInitializer {

    private static boolean enabled = false;
    private static boolean locked = true;

    private static final String KEY = "DALI";
    private static final HashMap<UUID, Integer> warnings = new HashMap<>();

    @Override
    public void onInitialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // 🔑 مفتاح القفل
            dispatcher.register(
                CommandManager.literal("daliscriptkey")
                    .then(CommandManager.argument("key", StringArgumentType.string())
                        .executes(ctx -> {

                            String input = StringArgumentType.getString(ctx, "key");

                            if (input.equals(KEY)) {
                                locked = false;
                                ctx.getSource().sendFeedback(() ->
                                        Text.literal("§aDaliScript V6 UNLOCKED"), false);
                            } else {
                                locked = true;
                                ctx.getSource().sendFeedback(() ->
                                        Text.literal("§4SYSTEM LOCKED"), false);
                            }

                            return 1;
                        }))
            );

            // ⚙️ تشغيل / إيقاف
            dispatcher.register(
                CommandManager.literal("daliscript")
                    .executes(ctx -> {

                        if (locked) {
                            ctx.getSource().sendFeedback(() ->
                                    Text.literal("§cLOCKED - USE KEY"), false);
                            return 0;
                        }

                        enabled = !enabled;

                        ctx.getSource().sendFeedback(() ->
                                Text.literal(enabled ? "§aV6 ONLINE" : "§cV6 OFFLINE"), false);

                        return 1;
                    })
            );

            // 🎮 أمر الجيم مود الجديد
            dispatcher.register(
                CommandManager.literal("daliscript")
                    .then(CommandManager.literal("gamemode")
                        .then(CommandManager.argument("mode", IntegerArgumentType.integer(0, 3))
                            .executes(ctx -> {

                                if (!enabled || locked) {
                                    ctx.getSource().sendFeedback(() ->
                                            Text.literal("§cSYSTEM NOT ACTIVE"), false);
                                    return 0;
                                }

                                int mode = IntegerArgumentType.getInteger(ctx, "mode");

                                ServerPlayerEntity player = ctx.getSource().getPlayer();

                                if (player == null) return 0;

                                GameMode gm = switch (mode) {
                                    case 0 -> GameMode.SURVIVAL;
                                    case 1 -> GameMode.CREATIVE;
                                    case 2 -> GameMode.ADVENTURE;
                                    case 3 -> GameMode.SPECTATOR;
                                    default -> GameMode.SURVIVAL;
                                };

                                player.changeGameMode(gm);

                                ctx.getSource().sendFeedback(() ->
                                        Text.literal("§aGamemode set to " + gm.name()), false);

                                return 1;
                            }))
                    )
            );

        });

        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    private void tick(MinecraftServer server) {

        if (!enabled) return;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

            UUID id = player.getUuid();

            // 💀 Hardcore rules
            server.getCommandManager().executeWithPrefix(
                    server.getCommandSource(),
                    "difficulty hard"
            );

            server.getCommandManager().executeWithPrefix(
                    server.getCommandSource(),
                    "gamerule keepInventory false"
            );

            // 🛡 Anti-cheat
            if (player.getAbilities().flying) {
                player.getAbilities().flying = false;
                warn(player, "Fly detected");
            }

            if (player.getVelocity().length() > 1.3) {
                player.setVelocity(0, 0, 0);
                warn(player, "Speed hack detected");
            }

            // منع creative override (إذا ما استخدم الأمر)
            if (player.isCreative() && locked) {
                player.changeGameMode(GameMode.SURVIVAL);
            }

            // 📊 تحذيرات
            int w = warnings.getOrDefault(id, 0);

            if (w >= 5) {
                player.networkHandler.disconnect(Text.literal("BANNED BY DaliScript V6"));
            }
        }
    }

    private void warn(ServerPlayerEntity player, String reason) {
        UUID id = player.getUuid();

        int w = warnings.getOrDefault(id, 0) + 1;
        warnings.put(id, w);

        player.sendMessage(Text.literal("§c[WARN] " + reason + " (" + w + "/5)"), false);
    }
}