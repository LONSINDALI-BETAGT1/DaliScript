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

    // 💰 SIMPLE BALANCE SYSTEM (Emeralds)
    private static final Map<UUID, Integer> balance = new HashMap<>();

    @Override
    public void onInitialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // 🛒 OPEN SHOP
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("shop")
                    .executes(ctx -> {

                        ServerPlayerEntity p = ctx.getSource().getPlayer();

                        send(p,
                            "§6🛒 DaliShop V25\n" +
                            "1. Buy Nitro = 32 Emeralds\n" +
                            "2. Sell Items = /sell\n" +
                            "3. Balance = /balance"
                        );

                        return 1;
                    })
            );

            // 💰 BALANCE
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("balance")
                    .executes(ctx -> {

                        ServerPlayerEntity p = ctx.getSource().getPlayer();

                        int bal = balance.getOrDefault(p.getUuid(), 0);

                        send(p, "§a💰 Balance: " + bal + " Emeralds");

                        return 1;
                    })
            );

            // 💎 BUY NITRO
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("buy")
                    .then(net.minecraft.server.command.CommandManager.literal("nitro")
                        .executes(ctx -> {

                            ServerPlayerEntity p = ctx.getSource().getPlayer();

                            int bal = balance.getOrDefault(p.getUuid(), 0);

                            if (bal < 32) {
                                send(p, "§c❌ Not enough Emeralds");
                                return 1;
                            }

                            balance.put(p.getUuid(), bal - 32);

                            send(p, "§d💎 Nitro Purchased!");

                            return 1;
                        }))
            );

            // 📦 SELL ITEM (Inventory-based)
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("sell")
                    .executes(ctx -> {

                        ServerPlayerEntity p = ctx.getSource().getPlayer();

                        Inventory inv = p.getInventory();

                        int earned = 0;

                        for (int i = 0; i < inv.size(); i++) {

                            ItemStack stack = inv.getStack(i);

                            if (stack.isEmpty()) continue;

                            // 💎 Simple pricing
                            if (stack.getItem() == Items.DIAMOND) {
                                earned += stack.getCount() * 5;
                                inv.setStack(i, ItemStack.EMPTY);
                            }

                            if (stack.getItem() == Items.EMERALD) {
                                earned += stack.getCount();
                                inv.setStack(i, ItemStack.EMPTY);
                            }
                        }

                        int bal = balance.getOrDefault(p.getUuid(), 0);
                        balance.put(p.getUuid(), bal + earned);

                        send(p, "§a💰 Sold items for: " + earned + " Emeralds");

                        return 1;
                    })
            );

        });
    }

    // 💬 SEND
    private void send(ServerPlayerEntity p, String msg) {
        if (p != null) {
            p.sendMessage(Text.literal(msg), false);
        }
    }
}