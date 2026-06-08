package com.dali.daliscript;

import com.mojang.brigadier.arguments.BoolArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

public class DaliScriptMod implements ModInitializer {

    private static boolean enabled = false;

    @Override
    public void onInitialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            dispatcher.register(
                CommandManager.literal("daliscript")
                    .then(CommandManager.argument("state", BoolArgumentType.bool())
                        .executes(context -> {

                            enabled = BoolArgumentType.getBool(context, "state");
                            MinecraftServer server = context.getSource().getServer();

                            if (enabled) {
                                enableMode(server);
                                context.getSource().sendFeedback(
                                        () -> Text.literal("§aDaliScript ENABLED (Hardcore Mode ON)"),
                                        false
                                );
                            } else {
                                disableMode(server);
                                context.getSource().sendFeedback(
                                        () -> Text.literal("§cDaliScript DISABLED"),
                                        false
                                );
                            }

                            return 1;
                        }))
            );

        });
    }

    private void enableMode(MinecraftServer server) {
        server.getCommandManager().executeWithPrefix(
                server.getCommandSource(),
                "difficulty hard"
        );

        server.getCommandManager().executeWithPrefix(
                server.getCommandSource(),
                "gamerule doImmediateRespawn false"
        );

        server.getCommandManager().executeWithPrefix(
                server.getCommandSource(),
                "gamerule keepInventory false"
        );
    }

    private void disableMode(MinecraftServer server) {
        server.getCommandManager().executeWithPrefix(
                server.getCommandSource(),
                "difficulty normal"
        );
    }
}