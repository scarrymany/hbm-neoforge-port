package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineBAT9000BlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** Tank + fluid-id (CE BAT9000 barrel GUI). */
public class BAT9000Menu extends MenuBase<MachineBAT9000BlockEntity> {

    public BAT9000Menu(int id, Inventory playerInv, MachineBAT9000BlockEntity be) {
        super(DummyableProcessMenus.MACHINE_BAT9000.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 80, 54));
        playerInv(playerInv, 8, 84);
    }
}
