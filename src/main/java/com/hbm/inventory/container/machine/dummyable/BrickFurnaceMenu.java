package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineBrickFurnaceBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerFurnaceBrick}: in / fuel / out. Ash slot unused. */
public class BrickFurnaceMenu extends MenuBase<MachineBrickFurnaceBlockEntity> {

    public BrickFurnaceMenu(int id, Inventory playerInv, MachineBrickFurnaceBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_BRICK_FURNACE.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 56, 17));
        this.addSlot(new SlotNonRetarded(tile, 1, 56, 53));
        this.addSlot(new SlotTakeOnly(tile, 2, 116, 35));
        playerInv(playerInv, 8, 86);
    }
}
