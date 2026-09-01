package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.FurnaceIronBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerFurnaceIron}: in 26,36 / fuel 62+80,36 / out 116,36 / upgrade 152,36. */
public class FurnaceIronMenu extends MenuBase<FurnaceIronBlockEntity> {

    public FurnaceIronMenu(int id, Inventory playerInv, FurnaceIronBlockEntity be) {
        super(DummyableProcessMenus.FURNACE_IRON.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 26, 36));
        this.addSlot(new SlotNonRetarded(tile, 1, 62, 36));
        this.addSlot(new SlotNonRetarded(tile, 2, 80, 36));
        this.addSlot(new SlotTakeOnly(tile, 3, 116, 36));
        this.addSlot(new SlotNonRetarded(tile, 4, 152, 36));
        playerInv(playerInv, 8, 86);
    }
}
