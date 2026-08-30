package com.hbm.inventory.container.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKBoilerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/** Boiler menu - no slots, tank fill read directly off {@link #be} by the matching Screen. */
public class RBMKBoilerMenu extends RBMKSlottedMenuBase<RBMKBoilerBlockEntity> {

    public RBMKBoilerMenu(int id, Inventory playerInventory, RBMKBoilerBlockEntity be) {
        super(RBMKMenuTypes.BOILER.get(), id, be);
        playerInv(playerInventory, 8, 84);
    }

    public static RBMKBoilerMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof RBMKBoilerBlockEntity be) {
            return new RBMKBoilerMenu(id, playerInventory, be);
        }
        throw new IllegalStateException("No RBMKBoilerBlockEntity at " + pos);
    }
}
