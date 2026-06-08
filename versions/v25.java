package com.dali.daliscript;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.Inventory;

import java.util.*;

public class DaliScriptMod implements ModInitializer {

    // 💰 BALANCE SYSTEM
    private static final Map<UUID, Integer> balance = new HashMap<>();

    // 📊 PRICE SYSTEM (Dynamic)
    private static int diamondPrice = 5;

    // ⏳ COOLDOWN SYSTEM
    private static final Map<UUID, Long> cooldown = new HashMap<>();

    // 📦 AUCTION SYSTEM
    private static final Map<UUID, Integer> auction = new HashMap<>();

    @Override
    public void onInitialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // 🛒 SHOP
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("shop")
                    .executes(ctx -> {

                        ServerPlayerEntity p = ctx.getSource().getPlayer();

                        send(p,
                            "§6🛒 DaliMarket V26\n" +
                            "- /buy nitro (32 Emeralds)\n" +
                            "- /sell (inventory)\n" +
                            "- /trade\n" +
                            "- /auction"
                        );

                        return 1;
                    })
            );

            // 💰 BALANCE
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("balance")
                    .executes(ctx -> {

                        ServerPlayerEntity p = ctx.getSource().getPlayer();

                        send(p, "§a💰 Balance: " + balance.getOrDefault(p.getUuid(), 0));

                        return 1;
                    })
            );

            // 💎 BUY NITRO
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("buy")
                    .then(net.minecraft.server.command.CommandManager.literal("nitro")
                        .executes(ctx -> {

                            ServerPlayerEntity p = ctx.getSource().getPlayer();

                            if (!canUse(p)) return 1;

                            int bal = balance.getOrDefault(p.getUuid(), 0);

                            if (bal < 32) {
                                send(p, "§c❌ Not enough Emeralds");
                                return 1;
                            }

                            balance.put(p.getUuid(), bal - 32);

                            adjustMarket(true); // 📈 demand increases

                            send(p, "§d💎 Nitro Purchased");

                            return 1;
                        }))
            );

            // 📦 SELL INVENTORY
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("sell")
                    .executes(ctx -> {

                        ServerPlayerEntity p = ctx.getSource().getPlayer();

                        if (!canUse(p)) return 1;

                        Inventory inv = p.getInventory();

                        int earned = 0;

                        for (int i = 0; i < inv.size(); i++) {

                            ItemStack stack = inv.getStack(i);

                            if (stack.isEmpty()) continue;

                            if (stack.getItem() == Items.DIAMOND) {
                                earned += stack.getCount() * diamondPrice;
                                inv.setStack(i, ItemStack.EMPTY);
                            }

                            if (stack.getItem() == Items.EMERALD) {
                                earned += stack.getCount();
                                inv.setStack(i, ItemStack.EMPTY);
                            }
                        }

                        balance.put(p.getUuid(),
                                balance.getOrDefault(p.getUuid(), 0) + earned);

                        adjustMarket(false); // 📉 supply increases

                        send(p, "§a💰 Sold for: " + earned);

                        return 1;
                    })
            );

            // 🔁 TRADE SYSTEM
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("trade")
                    .then(net.minecraft.server.command.CommandManager.argument("player",
                        com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(ctx -> {

                            ServerPlayerEntity p = ctx.getSource().getPlayer();

                            send(p, "§e🔁 Trade request sent (SIMULATION)");

                            return 1;
                        }))
            );

            // 📦 AUCTION
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("auction")
                    .then(net.minecraft.server.command.CommandManager.literal("sell")
                        .executes(ctx -> {

                            ServerPlayerEntity p = ctx.getSource().getPlayer();

                            auction.put(p.getUuid(), 100);

                            send(p, "§6📦 Item listed in Auction for 100+");

                            return 1;
                        }))
            );

        });
    }

    // ⚡ MARKET ENGINE (PRICE FLUCTUATION)
    private void adjustMarket(boolean demandUp) {

        if (demandUp) {
            diamondPrice += 1;
        } else {
            diamondPrice = Math.max(1, diamondPrice - 1);
        }
    }

    // ⏳ COOLDOWN CHECK
    private boolean canUse(ServerPlayerEntity p) {

        long now = System.currentTimeMillis();
        long last = cooldown.getOrDefault(p.getUuid(), 0L);

        if (now - last < 3000) {
            send(p, "§c⏳ Wait cooldown!");
            return false;
        }

        cooldown.put(p.getUuid(), now);
        return true;
    }

    // 💬 SEND
    private void send(ServerPlayerEntity p, String msg) {
        if (p != null) {
            p.sendMessage(Text.literal(msg), false);
        }
    }
}