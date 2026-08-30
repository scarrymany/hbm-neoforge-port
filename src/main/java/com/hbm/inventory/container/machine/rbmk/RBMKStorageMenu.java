package com.hbm.inventory.container.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/** Spent/spare rod storage menu - 3x3 grid. */
public class RBMKStorageMenu extends RBMKSlottedMenuBase<RBMKStorageBlockEntity> {

    public RBMKStorageMenu(int id, Inventory playerInventory, RBMKStorageBlockEntity be) {
        super(RBMKMenuTypes.STORAGE.get(), id, be);
        addSlots(be.inventory, 0, 62, 17, 3, 3);
        playerInv(playerInventory, 8, 84);
    }

    public static RBMKStorageMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof RBMKStorageBlockEntity be) {
            return new RBMKStorageMenu(id, playerInventory, be);
        }
        throw new IllegalStateException("No RBMKStorageBlockEntity at " + pos);
    }
}
