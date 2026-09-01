package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineAshpitBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerAshpit} 5 output slots. */
public class AshpitMenu extends MenuBase<MachineAshpitBlockEntity> {

    public AshpitMenu(int id, Inventory playerInv, MachineAshpitBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_ASHPIT.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 44, 27));
        this.addSlot(new SlotNonRetarded(tile, 1, 62, 27));
        this.addSlot(new SlotNonRetarded(tile, 2, 80, 27));
        this.addSlot(new SlotNonRetarded(tile, 3, 98, 27));
        this.addSlot(new SlotNonRetarded(tile, 4, 116, 27));
        playerInv(playerInv, 8, 86);
    }
}
