package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineDrainBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** Tank + fluid-id (CE drain is overlay-only). */
public class DrainMenu extends MenuBase<MachineDrainBlockEntity> {

    public DrainMenu(int id, Inventory playerInv, MachineDrainBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_DRAIN.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 80, 54));
        playerInv(playerInv, 8, 84);
    }
}
