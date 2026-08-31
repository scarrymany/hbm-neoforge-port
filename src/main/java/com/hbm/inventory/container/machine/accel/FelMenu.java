package com.hbm.inventory.container.machine.accel;

import com.hbm.blockentity.machine.accel.FelBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerFEL}: battery + crystal. */
public class FelMenu extends MenuBase<FelBlockEntity> {

    public FelMenu(int id, Inventory playerInv, FelBlockEntity be) {
        super(AccelMenus.MACHINE_FEL.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 8, 21));
        this.addSlot(new SlotNonRetarded(tile, 1, 80, 39));
        playerInv(playerInv, 8, 104);
    }
}
