package com.hbm.inventory.container.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKHeaterBlockEntity;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/** Exact CE {@code ContainerRBMKHeater.java:24}: fluid ID 41,45. Player inv +20 (186 GUI). */
public class RBMKHeaterMenu extends RBMKSlottedMenuBase<RBMKHeaterBlockEntity> {

    public RBMKHeaterMenu(int id, Inventory playerInventory, RBMKHeaterBlockEntity be) {
        super(RBMKMenuTypes.HEATER.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 41, 45));
        playerInv(playerInventory, 8, 104);
    }

    public static RBMKHeaterMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof RBMKHeaterBlockEntity be) {
            return new RBMKHeaterMenu(id, playerInventory, be);
        }
        throw new IllegalStateException("No RBMKHeaterBlockEntity at " + pos);
    }
}
