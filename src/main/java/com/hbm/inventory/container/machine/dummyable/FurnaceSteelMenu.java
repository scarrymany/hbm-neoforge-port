package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.FurnaceSteelBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerFurnaceSteel}: 3 in 35/53/71,17 / 3 out 35/53/71,63. */
public class FurnaceSteelMenu extends MenuBase<FurnaceSteelBlockEntity> {

    public FurnaceSteelMenu(int id, Inventory playerInv, FurnaceSteelBlockEntity be) {
        super(DummyableProcessMenus.FURNACE_STEEL.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 35, 17));
        this.addSlot(new SlotNonRetarded(tile, 1, 53, 17));
        this.addSlot(new SlotNonRetarded(tile, 2, 71, 17));
        this.addSlot(new SlotTakeOnly(tile, 3, 35, 63));
        this.addSlot(new SlotTakeOnly(tile, 4, 53, 63));
        this.addSlot(new SlotTakeOnly(tile, 5, 71, 63));
        playerInv(playerInv, 8, 104);
    }
}
