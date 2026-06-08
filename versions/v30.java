package com.dali.daliscript;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class DaliScriptMod implements ModInitializer {

    @Override
    public void onInitialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // 🪟 OPEN DESKTOP
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("dalios")
                    .executes(ctx -> {

                        MinecraftClient client = MinecraftClient.getInstance();

                        if (client.player != null) {
                            client.setScreen(new DaliDesktopScreen());
                        }

                        return 1;
                    })
            );
        });
    }

    // 🪟 MAIN DESKTOP SCREEN
    public static class DaliDesktopScreen extends Screen {

        protected DaliDesktopScreen() {
            super(Text.literal("DaliOS V31 Desktop"));
        }

        @Override
        protected void init() {

            int y = this.height / 4;

            // 🧠 AI
            this.addDrawableChild(ButtonWidget.builder(Text.literal("🧠 AI Chat"), b -> {
                MinecraftClient.getInstance().player.sendMessage(Text.literal("AI Window Opened"), false);
            }).dimensions(this.width / 2 - 75, y, 150, 20).build());

            // 🛒 SHOP
            this.addDrawableChild(ButtonWidget.builder(Text.literal("🛒 Shop"), b -> {
                MinecraftClient.getInstance().player.sendMessage(Text.literal("Shop Window Opened"), false);
            }).dimensions(this.width / 2 - 75, y + 25, 150, 20).build());

            // 🏦 BANK
            this.addDrawableChild(ButtonWidget.builder(Text.literal("🏦 Bank"), b -> {
                MinecraftClient.getInstance().player.sendMessage(Text.literal("Bank Window Opened"), false);
            }).dimensions(this.width / 2 - 75, y + 50, 150, 20).build());

            // 📝 NOTES
            this.addDrawableChild(ButtonWidget.builder(Text.literal("📝 Notes"), b -> {
                MinecraftClient.getInstance().player.sendMessage(Text.literal("Notes Window Opened"), false);
            }).dimensions(this.width / 2 - 75, y + 75, 150, 20).build());

            // ⏰ ALARM
            this.addDrawableChild(ButtonWidget.builder(Text.literal("⏰ Alarm"), b -> {
                MinecraftClient.getInstance().player.sendMessage(Text.literal("Alarm Window Opened"), false);
            }).dimensions(this.width / 2 - 75, y + 100, 150, 20).build());

            // ❌ CLOSE
            this.addDrawableChild(ButtonWidget.builder(Text.literal("❌ Close"), b -> {
                MinecraftClient.getInstance().setScreen(null);
            }).dimensions(this.width / 2 - 75, y + 140, 150, 20).build());
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }
}