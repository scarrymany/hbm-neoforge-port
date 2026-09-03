package com.hbm.inventory.container.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKHeaterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/** Heater menu - no slots, tank fill read directly off {@link #be} by the matching Screen. */
public class RBMKHeaterMenu extends RBMKSlottedMenuBase<RBMKHeaterBlockEntity> {

    public RBMKHeaterMenu(int id, Inventory playerInventory, RBMKHeaterBlockEntity be) {
        super(RBMKMenuTypes.HEATER.get(), id, be);
        playerInv(playerInventory, 8, 84);
    }

    public static RBMKHeaterMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof RBMKHeaterBlockEntity be) {
            return new RBMKHeaterMenu(id, playerInventory, be);
        }
        throw new IllegalStateException("No RBMKHeaterBlockEntity at " + pos);
    }
}
