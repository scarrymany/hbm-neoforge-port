package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.FurnaceSteelBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code ContainerFurnaceSteel}: 3 in 35,17/35/53 / 3 out 125,17/35/53.
 * Invented horizontal 35/53/71 layout removed.
 */
public class FurnaceSteelMenu extends MenuBase<FurnaceSteelBlockEntity> {

    public FurnaceSteelMenu(int id, Inventory playerInv, FurnaceSteelBlockEntity be) {
        super(DummyableProcessMenus.FURNACE_STEEL.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 35, 17));
        this.addSlot(new SlotNonRetarded(tile, 1, 35, 35));
        this.addSlot(new SlotNonRetarded(tile, 2, 35, 53));
        this.addSlot(new SlotTakeOnly(tile, 3, 125, 17));
        this.addSlot(new SlotTakeOnly(tile, 4, 125, 35));
        this.addSlot(new SlotTakeOnly(tile, 5, 125, 53));
        playerInv(playerInv, 8, 84);
    }
}
