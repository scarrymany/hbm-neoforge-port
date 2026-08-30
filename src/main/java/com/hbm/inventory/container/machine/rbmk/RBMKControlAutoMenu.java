package com.hbm.inventory.container.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKControlAutoBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/** Auto control rod menu - no slots, heat/level bounds set via {@link com.hbm.interfaces.IControlReceiver}. */
public class RBMKControlAutoMenu extends RBMKSlottedMenuBase<RBMKControlAutoBlockEntity> {

    public RBMKControlAutoMenu(int id, Inventory playerInventory, RBMKControlAutoBlockEntity be) {
        super(RBMKMenuTypes.CONTROL_AUTO.get(), id, be);
        playerInv(playerInventory, 8, 84);
    }

    public static RBMKControlAutoMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof RBMKControlAutoBlockEntity be) {
            return new RBMKControlAutoMenu(id, playerInventory, be);
        }
        throw new IllegalStateException("No RBMKControlAutoBlockEntity at " + pos);
    }
}
