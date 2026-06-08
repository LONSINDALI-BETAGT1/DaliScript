package com.dali.daliscript;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

public class DaliScriptMod implements ModInitializer {

    @Override
    public void onInitialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {

            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("dali")
                    .executes(ctx -> {
                        ctx.getSource().getPlayer().sendMessage(
                            Text.literal("💀 DaliScript V2 Active"), false);
                        return 1;
                    })
            );

            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("gm")
                    .then(net.minecraft.server.command.CommandManager.argument(
                        "mode", IntegerArgumentType.integer(0, 3)
                    ).executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        int mode = IntegerArgumentType.getInteger(ctx, "mode");
                        switch (mode) {
                            case 0 -> { p.changeGameMode(GameMode.SURVIVAL); send(p, "SURVIVAL"); }
                            case 1 -> { p.changeGameMode(GameMode.CREATIVE); send(p, "CREATIVE"); }
                            case 2 -> { p.changeGameMode(GameMode.ADVENTURE); send(p, "ADVENTURE"); }
                            case 3 -> { p.changeGameMode(GameMode.SPECTATOR); send(p, "SPECTATOR"); }
                        }
                        return 1;
                    }))
            );

            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("ai")
                    .then(net.minecraft.server.command.CommandManager.argument(
                        "q", StringArgumentType.greedyString()
                    ).executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        String q = StringArgumentType.getString(ctx, "q").toLowerCase();
                        if (q.contains("diamond")) send(p, "Y = -58");
                        else if (q.contains("mob")) send(p, "Warden is strongest");
                        else send(p, "No data available");
                        return 1;
                    }))
            );

        });
    }

    private void send(ServerPlayerEntity p, String msg) {
        p.sendMessage(Text.literal("💀 " + msg), false);
    }
}