package com.hbm.inventory.container.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKOutgasserBlockEntity;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * CE {@code ContainerRBMKOutgasser}: slot 0 input @ (48,53), slot 1 take-only @ (112,53).
 */
public class RBMKOutgasserMenu extends RBMKSlottedMenuBase<RBMKOutgasserBlockEntity> {

    public RBMKOutgasserMenu(int id, Inventory playerInventory, RBMKOutgasserBlockEntity be) {
        super(RBMKMenuTypes.OUTGASSER.get(), id, be);
        addSlots(be.inventory, 0, 48, 53, 1, 1);
        addSlot(new SlotTakeOnly(be.inventory, 1, 112, 53));
        playerInv(playerInventory, 8, 104);
    }

    public static RBMKOutgasserMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof RBMKOutgasserBlockEntity be) {
            return new RBMKOutgasserMenu(id, playerInventory, be);
        }
        throw new IllegalStateException("No RBMKOutgasserBlockEntity at " + pos);
    }
}
