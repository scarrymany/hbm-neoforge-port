package com.hbm.inventory.container.machine.accel;

import com.hbm.blockentity.machine.accel.PaPartBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** CE PA part containers: battery + coil. */
public class PaPartMenu extends MenuBase<PaPartBlockEntity> {

    public PaPartMenu(int id, Inventory playerInv, PaPartBlockEntity be) {
        super(AccelMenus.PA_PART.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 8, 21));
        this.addSlot(new SlotNonRetarded(tile, 1, 80, 39));
        playerInv(playerInv, 8, 104);
    }
}
