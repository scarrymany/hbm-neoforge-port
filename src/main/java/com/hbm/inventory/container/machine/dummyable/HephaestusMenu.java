package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineHephaestusBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** CE overlay-only Hephaestus — ID slot + live tanks/heat (not a stub). */
public class HephaestusMenu extends MenuBase<MachineHephaestusBlockEntity> {

    public HephaestusMenu(int id, Inventory playerInv, MachineHephaestusBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_HEPHAESTUS.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 8, 54));
        playerInv(playerInv, 8, 84, 142);
    }
}
