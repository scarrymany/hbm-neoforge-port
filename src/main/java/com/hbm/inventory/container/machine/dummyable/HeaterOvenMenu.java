package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.HeaterOvenBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** Exact CE {@code ContainerFirebox} reused for the oven: fuel 44,27 + 62,27. */
public class HeaterOvenMenu extends MenuBase<HeaterOvenBlockEntity> {

    public HeaterOvenMenu(int id, Inventory playerInv, HeaterOvenBlockEntity be) {
        super(DummyableProcessMenus.HEATER_OVEN.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 44, 27));
        this.addSlot(new SlotNonRetarded(tile, 1, 62, 27));
        playerInv(playerInv, 8, 86);
    }
}
