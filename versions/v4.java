package com.dali.daliscript;

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

    // 🔑 مفتاح “مشفر بسيط”
    private static final String HASH_KEY = "4A4C49"; // DALI hex

    // 📊 نظام عقوبات
    private static final HashMap<UUID, Integer> warnings = new HashMap<>();

    @Override
    public void onInitialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // 🔑 أمر المفتاح
            dispatcher.register(
                CommandManager.literal("daliscriptkey")
                    .then(CommandManager.argument("key", StringArgumentType.string())
                        .executes(ctx -> {

                            String input = StringArgumentType.getString(ctx, "key");
                            String hex = toHex(input);

                            if (hex.equals(HASH_KEY)) {
                                locked = false;
                                ctx.getSource().sendFeedback(() ->
                                        Text.literal("§aACCESS GRANTED"), false);
                            } else {
                                locked = true;
                                ctx.getSource().sendFeedback(() ->
                                        Text.literal("§4ACCESS DENIED"), false);
                            }

                            return 1;
                        }))
            );

            // ⚙️ تشغيل النظام
            dispatcher.register(
                CommandManager.literal("daliscript")
                    .executes(ctx -> {

                        if (locked) {
                            ctx.getSource().sendFeedback(() ->
                                    Text.literal("§cSYSTEM LOCKED"), false);
                            return 0;
                        }

                        enabled = !enabled;

                        ctx.getSource().sendFeedback(() ->
                                Text.literal(enabled ? "§aV5 SYSTEM ONLINE" : "§cSYSTEM OFFLINE"), false);

                        return 1;
                    })
            );

        });

        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    private void tick(MinecraftServer server) {

        if (!enabled) return;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

            UUID id = player.getUuid();

            // 💀 Hardcore enforcement
            server.getCommandManager().executeWithPrefix(server.getCommandSource(), "difficulty hard");

            // 🧠 منع creative
            if (player.isCreative()) {
                player.changeGameMode(GameMode.SURVIVAL);
                warn(player, "Creative mode blocked");
            }

            // 🛡 Fly detection
            if (player.getAbilities().flying) {
                player.getAbilities().flying = false;
                warn(player, "Flying detected");
            }

            // 🛡 Speed detection
            if (player.getVelocity().length() > 1.3) {
                player.setVelocity(0, 0, 0);
                warn(player, "Speed hack detected");
            }

            // 📊 نظام التحذيرات
            int w = warnings.getOrDefault(id, 0);

            if (w >= 5) {
                player.networkHandler.disconnect(Text.literal("Banned by DaliScript V5"));
            }
        }
    }

    private void warn(ServerPlayerEntity player, String reason) {
        UUID id = player.getUuid();

        int w = warnings.getOrDefault(id, 0) + 1;
        warnings.put(id, w);

        player.sendMessage(Text.literal("§c[WARNING] " + reason + " (" + w + "/5)"), false);
    }

    private String toHex(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            sb.append(Integer.toHexString(c).toUpperCase());
        }
        return sb.toString();
    }
}