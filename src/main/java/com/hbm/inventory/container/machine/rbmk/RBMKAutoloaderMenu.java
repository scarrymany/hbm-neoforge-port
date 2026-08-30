package com.hbm.inventory.container.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKAutoloaderBlockEntity;
import com.hbm.inventory.container.MenuBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/** Autoloader menu - 2-slot rod hopper, reuses the shared {@link MenuBase} ({@code MachineBaseBlockEntity}-backed). */
public class RBMKAutoloaderMenu extends MenuBase<RBMKAutoloaderBlockEntity> {

    public RBMKAutoloaderMenu(int id, Inventory playerInventory, RBMKAutoloaderBlockEntity be) {
        super(RBMKMenuTypes.AUTOLOADER.get(), id, be);
        addSlots(be.getItemHandlerCapability(null), 0, 71, 20, 1, 2);
        playerInv(playerInventory, 8, 84);
    }

    public static RBMKAutoloaderMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof RBMKAutoloaderBlockEntity be) {
            return new RBMKAutoloaderMenu(id, playerInventory, be);
        }
        throw new IllegalStateException("No RBMKAutoloaderBlockEntity at " + pos);
    }
}
