package com.hbm.inventory.container.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKControlManualBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/** Manual control rod menu - no slots, target-level/color set via {@link com.hbm.interfaces.IControlReceiver}. */
public class RBMKControlMenu extends RBMKSlottedMenuBase<RBMKControlManualBlockEntity> {

    public RBMKControlMenu(int id, Inventory playerInventory, RBMKControlManualBlockEntity be) {
        super(RBMKMenuTypes.CONTROL.get(), id, be);
        playerInv(playerInventory, 8, 84);
    }

    public static RBMKControlMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof RBMKControlManualBlockEntity be) {
            return new RBMKControlMenu(id, playerInventory, be);
        }
        throw new IllegalStateException("No RBMKControlManualBlockEntity at " + pos);
    }
}
