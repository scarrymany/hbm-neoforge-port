package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.WasteDrumBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerWasteDrum} 12-slot pool. */
public class WasteDrumMenu extends MenuBase<WasteDrumBlockEntity> {

    public WasteDrumMenu(int id, Inventory playerInv, WasteDrumBlockEntity be) {
        super(DummyableProcessMenus.WASTE_DRUM.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 71, 21));
        this.addSlot(new SlotNonRetarded(tile, 1, 89, 21));
        this.addSlot(new SlotNonRetarded(tile, 2, 53, 39));
        this.addSlot(new SlotNonRetarded(tile, 3, 71, 39));
        this.addSlot(new SlotNonRetarded(tile, 4, 89, 39));
        this.addSlot(new SlotNonRetarded(tile, 5, 107, 39));
        this.addSlot(new SlotNonRetarded(tile, 6, 53, 57));
        this.addSlot(new SlotNonRetarded(tile, 7, 71, 57));
        this.addSlot(new SlotNonRetarded(tile, 8, 89, 57));
        this.addSlot(new SlotNonRetarded(tile, 9, 107, 57));
        this.addSlot(new SlotNonRetarded(tile, 10, 71, 75));
        this.addSlot(new SlotNonRetarded(tile, 11, 89, 75));
        playerInv(playerInv, 8, 112);
    }
}
