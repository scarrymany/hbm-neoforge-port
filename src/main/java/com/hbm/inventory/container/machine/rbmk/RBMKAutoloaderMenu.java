package com.hbm.inventory.container.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKAutoloaderBlockEntity;
import com.hbm.inventory.container.MenuBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * Autoloader menu. Exact CE {@code ContainerRBMKAutoloader.java:17-19}: 3×3 in, 3×3 take-only out,
 * player inv at 8,100.
 */
public class RBMKAutoloaderMenu extends MenuBase<RBMKAutoloaderBlockEntity> {

    public RBMKAutoloaderMenu(int id, Inventory playerInventory, RBMKAutoloaderBlockEntity be) {
        super(RBMKMenuTypes.AUTOLOADER.get(), id, be);
        addSlots(be.inventory, 0, 17, 18, 3, 3);
        addTakeOnlySlots(be.inventory, 9, 107, 18, 3, 3);
        playerInv(playerInventory, 8, 100);
    }

    public static RBMKAutoloaderMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof RBMKAutoloaderBlockEntity be) {
            return new RBMKAutoloaderMenu(id, playerInventory, be);
        }
        throw new IllegalStateException("No RBMKAutoloaderBlockEntity at " + pos);
    }
}
