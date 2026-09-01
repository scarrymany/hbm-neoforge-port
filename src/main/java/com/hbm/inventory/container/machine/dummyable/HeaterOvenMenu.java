package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.HeaterOvenBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerFirebox} reused for the oven. */
public class HeaterOvenMenu extends MenuBase<HeaterOvenBlockEntity> {

    public HeaterOvenMenu(int id, Inventory playerInv, HeaterOvenBlockEntity be) {
        super(DummyableProcessMenus.HEATER_OVEN.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 26, 36));
        this.addSlot(new SlotNonRetarded(tile, 1, 44, 36));
        playerInv(playerInv, 8, 86);
    }
}
