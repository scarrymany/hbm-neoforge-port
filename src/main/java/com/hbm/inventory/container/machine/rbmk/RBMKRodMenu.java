package com.hbm.inventory.container.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKRodBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/** Fuel rod channel loading menu - one slot for the {@code ItemRBMKRod}. */
public class RBMKRodMenu extends RBMKSlottedMenuBase<RBMKRodBlockEntity> {

    public RBMKRodMenu(int id, Inventory playerInventory, RBMKRodBlockEntity be) {
        super(RBMKMenuTypes.ROD.get(), id, be);
        addSlots(be.inventory, 0, 80, 35, 1, 1);
        playerInv(playerInventory, 8, 84);
    }

    public static RBMKRodMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof RBMKRodBlockEntity be) {
            return new RBMKRodMenu(id, playerInventory, be);
        }
        throw new IllegalStateException("No RBMKRodBlockEntity at " + pos);
    }
}
