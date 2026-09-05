package com.hbm.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Dedicated-server-safe screen opens. Common item classes must not mention
 * {@code net.minecraft.client.*} — RuntimeDistCleaner rejects those classrefs at RegisterEvent.
 */
public final class ClientScreens {

    private ClientScreens() {
    }

    public static void turretMobFilter(BlockPos corePos) {
        net.minecraft.client.Minecraft.getInstance().setScreen(
                new com.hbm.inventory.gui.turret.TurretMobFilterScreen(corePos));
    }

    public static void satCoord(int freq) {
        net.minecraft.client.Minecraft.getInstance().setScreen(
                new com.hbm.inventory.gui.SatCoordScreen(freq));
    }

    public static void satInterface(int freq) {
        net.minecraft.client.Minecraft.getInstance().setScreen(
                new com.hbm.inventory.gui.SatInterfaceScreen(freq));
    }

    public static void designatorManual(ItemStack stack) {
        net.minecraft.client.Minecraft.getInstance().setScreen(
                new com.hbm.inventory.gui.DesignatorManualScreen(stack));
    }

    public static void fluidIdentifier(Player player) {
        net.minecraft.client.Minecraft.getInstance().setScreen(
                new com.hbm.client.gui.screens.GUIScreenFluid(player));
    }

    public static boolean hasShiftDown() {
        return net.minecraft.client.gui.screens.Screen.hasShiftDown();
    }
}
