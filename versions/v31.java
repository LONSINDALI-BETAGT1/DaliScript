package com.dali.daliscript;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.*;

public class DaliScriptMod implements ModInitializer {

    // 🧠 LAST OPEN APP MEMORY
    private static String lastApp = "home";

    @Override
    public void onInitialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("dalios")
                    .executes(ctx -> {

                        MinecraftClient client = MinecraftClient.getInstance();

                        if (client.player != null) {
                            client.setScreen(new DesktopScreen());
                        }

                        return 1;
                    })
            );
        });
    }

    // 🪟 DESKTOP HOME
    public static class DesktopScreen extends Screen {

        protected DesktopScreen() {
            super(Text.literal("DaliOS V32 Desktop"));
        }

        @Override
        protected void init() {

            int y = this.height / 4;

            // 🧠 AI WINDOW
            this.addDrawableChild(ButtonWidget.builder(Text.literal("🧠 AI"), b -> {
                lastApp = "ai";
                MinecraftClient.getInstance().setScreen(new AiWindow(this));
            }).dimensions(this.width / 2 - 75, y, 150, 20).build());

            // 🛒 SHOP WINDOW
            this.addDrawableChild(ButtonWidget.builder(Text.literal("🛒 Shop"), b -> {
                lastApp = "shop";
                MinecraftClient.getInstance().setScreen(new ShopWindow(this));
            }).dimensions(this.width / 2 - 75, y + 25, 150, 20).build());

            // 🏦 BANK WINDOW
            this.addDrawableChild(ButtonWidget.builder(Text.literal("🏦 Bank"), b -> {
                lastApp = "bank";
                MinecraftClient.getInstance().setScreen(new BankWindow(this));
            }).dimensions(this.width / 2 - 75, y + 50, 150, 20).build());

            // 📝 NOTES WINDOW
            this.addDrawableChild(ButtonWidget.builder(Text.literal("📝 Notes"), b -> {
                lastApp = "notes";
                MinecraftClient.getInstance().setScreen(new NotesWindow(this));
            }).dimensions(this.width / 2 - 75, y + 75, 150, 20).build());

            // ❌ CLOSE
            this.addDrawableChild(ButtonWidget.builder(Text.literal("❌ Exit"), b -> {
                MinecraftClient.getInstance().setScreen(null);
            }).dimensions(this.width / 2 - 75, y + 110, 150, 20).build());
        }
    }

    // 🧠 AI WINDOW
    public static class AiWindow extends Screen {
        private final Screen parent;

        protected AiWindow(Screen parent) {
            super(Text.literal("AI Window"));
            this.parent = parent;
        }

        @Override
        protected void init() {

            this.addDrawableChild(ButtonWidget.builder(Text.literal("💬 Ask AI"), b -> {
                MinecraftClient.getInstance().player.sendMessage(
                        Text.literal("🧠 AI: I am offline DaliAI system"), false);
            }).dimensions(this.width / 2 - 75, 80, 150, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal("⬅ Back"), b -> {
                MinecraftClient.getInstance().setScreen(parent);
            }).dimensions(this.width / 2 - 75, 110, 150, 20).build());
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }

    // 🛒 SHOP WINDOW
    public static class ShopWindow extends Screen {
        private final Screen parent;

        protected ShopWindow(Screen parent) {
            super(Text.literal("Shop Window"));
            this.parent = parent;
        }

        @Override
        protected void init() {

            this.addDrawableChild(ButtonWidget.builder(Text.literal("💎 Buy Nitro"), b -> {
                MinecraftClient.getInstance().player.sendMessage(
                        Text.literal("💎 Nitro purchased (simulation)"), false);
            }).dimensions(this.width / 2 - 75, 80, 150, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal("⬅ Back"), b -> {
                MinecraftClient.getInstance().setScreen(parent);
            }).dimensions(this.width / 2 - 75, 110, 150, 20).build());
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }

    // 🏦 BANK WINDOW
    public static class BankWindow extends Screen {
        private final Screen parent;

        protected BankWindow(Screen parent) {
            super(Text.literal("Bank Window"));
            this.parent = parent;
        }

        @Override
        protected void init() {

            this.addDrawableChild(ButtonWidget.builder(Text.literal("💰 Check Balance"), b -> {
                MinecraftClient.getInstance().player.sendMessage(
                        Text.literal("💰 Balance system active"), false);
            }).dimensions(this.width / 2 - 75, 80, 150, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal("⬅ Back"), b -> {
                MinecraftClient.getInstance().setScreen(parent);
            }).dimensions(this.width / 2 - 75, 110, 150, 20).build());
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }

    // 📝 NOTES WINDOW
    public static class NotesWindow extends Screen {
        private final Screen parent;

        protected NotesWindow(Screen parent) {
            super(Text.literal("Notes Window"));
            this.parent = parent;
        }

        @Override
        protected void init() {

            this.addDrawableChild(ButtonWidget.builder(Text.literal("📝 Save Note"), b -> {
                MinecraftClient.getInstance().player.sendMessage(
                        Text.literal("📝 Note saved (simulation)"), false);
            }).dimensions(this.width / 2 - 75, 80, 150, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal("⬅ Back"), b -> {
                MinecraftClient.getInstance().setScreen(parent);
            }).dimensions(this.width / 2 - 75, 110, 150, 20).build());
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }
}