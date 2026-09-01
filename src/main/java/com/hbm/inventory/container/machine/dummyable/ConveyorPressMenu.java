package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineConveyorPressBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** 1-slot stamp inventory for CE {@code TileEntityConveyorPress} (CE has no GUI; slot is real). */
public class ConveyorPressMenu extends MenuBase<MachineConveyorPressBlockEntity> {

    public ConveyorPressMenu(int id, Inventory playerInv, MachineConveyorPressBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_CONVEYOR_PRESS.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 80, 35));
        playerInv(playerInv, 8, 84);
    }
}
