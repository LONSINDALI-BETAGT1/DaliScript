package com.dali.daliscript;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.util.*;

public class DaliScriptMod implements ModInitializer {

    private static boolean phoneOpen = false;

    @Override
    public void onInitialize() {

        // 📱 OPEN PHONE COMMAND
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("daliphone")
                    .executes(ctx -> {

                        MinecraftClient client = MinecraftClient.getInstance();

                        if (client.player != null) {
                            client.setScreen(new DaliPhoneScreen());
                        }

                        return 1;
                    })
            );
        });
    }

    // 📱 DALI PHONE GUI SCREEN
    public static class DaliPhoneScreen extends Screen {

        protected DaliPhoneScreen() {
            super(Text.literal("DaliPhone V22"));
        }

        @Override
        protected void init() {

            int y = this.height / 4;

            // 💬 DALICORD BUTTON
            this.addDrawableChild(ButtonWidget.builder(Text.literal("💬 Dalicord"), b -> {
                MinecraftClient.getInstance().player.sendMessage(Text.literal("Open Dalicord UI"), false);
            }).dimensions(this.width / 2 - 75, y, 150, 20).build());

            // 📩 MESSAGE BUTTON
            this.addDrawableChild(ButtonWidget.builder(Text.literal("📩 DaliMessage"), b -> {
                MinecraftClient.getInstance().player.sendMessage(Text.literal("Open Messages UI"), false);
            }).dimensions(this.width / 2 - 75, y + 25, 150, 20).build());

            // 🛒 SHOP BUTTON
            this.addDrawableChild(ButtonWidget.builder(Text.literal("🛒 Shop"), b -> {
                MinecraftClient.getInstance().player.sendMessage(Text.literal("Shop: Nitro = 32 Diamonds"), false);
            }).dimensions(this.width / 2 - 75, y + 50, 150, 20).build());

            // 🧠 AI TRADER
            this.addDrawableChild(ButtonWidget.builder(Text.literal("🧠 AI Trader"), b -> {
                MinecraftClient.getInstance().player.sendMessage(Text.literal("AI: Prices stable"), false);
            }).dimensions(this.width / 2 - 75, y + 75, 150, 20).build());

            // ❌ CLOSE
            this.addDrawableChild(ButtonWidget.builder(Text.literal("❌ Close"), b -> {
                MinecraftClient.getInstance().setScreen(null);
            }).dimensions(this.width / 2 - 75, y + 110, 150, 20).build());
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }
}