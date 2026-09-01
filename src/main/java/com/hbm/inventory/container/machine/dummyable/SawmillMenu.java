package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineSawmillBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** Live 3-slot sawmill menu. CE was overlay-only. */
public class SawmillMenu extends MenuBase<MachineSawmillBlockEntity> {

    public SawmillMenu(int id, Inventory playerInv, MachineSawmillBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_SAWMILL.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 44, 36));
        this.addSlot(new SlotTakeOnly(tile, 1, 98, 27));
        this.addSlot(new SlotTakeOnly(tile, 2, 98, 45));
        playerInv(playerInv, 8, 86);
    }
}
