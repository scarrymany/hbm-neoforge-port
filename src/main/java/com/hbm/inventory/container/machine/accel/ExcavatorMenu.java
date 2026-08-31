package com.hbm.inventory.container.machine.accel;

import com.hbm.blockentity.machine.accel.ExcavatorBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachineExcavator}: drillbit + battery + 9 outputs. */
public class ExcavatorMenu extends MenuBase<ExcavatorBlockEntity> {

    public ExcavatorMenu(int id, Inventory playerInv, ExcavatorBlockEntity be) {
        super(AccelMenus.MACHINE_EXCAVATOR.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 80, 21));
        this.addSlot(new SlotNonRetarded(tile, 1, 8, 21));
        for (int i = 0; i < 9; i++) {
            this.addSlot(new SlotTakeOnly(tile, 2 + i, 8 + (i % 9) * 18, 57));
        }
        playerInv(playerInv, 8, 104);
    }
}
