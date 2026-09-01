package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineOrbusBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** Tank + fluid-id (CE orbus). */
public class OrbusMenu extends MenuBase<MachineOrbusBlockEntity> {

    public OrbusMenu(int id, Inventory playerInv, MachineOrbusBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_ORBUS.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 17, 17));
        playerInv(playerInv, 8, 84);
    }
}
