package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.FurnaceIronBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code ContainerFurnaceIron}: in 53,17 / fuel 53,53 + 71,53 / out 125,35 / upgrade 17,35.
 * Invented 26/62/80/116/152 row removed.
 */
public class FurnaceIronMenu extends MenuBase<FurnaceIronBlockEntity> {

    public FurnaceIronMenu(int id, Inventory playerInv, FurnaceIronBlockEntity be) {
        super(DummyableProcessMenus.FURNACE_IRON.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 53, 17));
        this.addSlot(new SlotNonRetarded(tile, 1, 53, 53));
        this.addSlot(new SlotNonRetarded(tile, 2, 71, 53));
        this.addSlot(new SlotTakeOnly(tile, 3, 125, 35));
        this.addSlot(new SlotNonRetarded(tile, 4, 17, 35));
        playerInv(playerInv, 8, 84);
    }
}
