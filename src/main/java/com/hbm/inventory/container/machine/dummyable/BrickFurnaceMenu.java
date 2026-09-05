package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineBrickFurnaceBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code ContainerFurnaceBrick}: in 62,35 / fuel 35,17 / out 116,35 / ash 35,53.
 */
public class BrickFurnaceMenu extends MenuBase<MachineBrickFurnaceBlockEntity> {

    public BrickFurnaceMenu(int id, Inventory playerInv, MachineBrickFurnaceBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_BRICK_FURNACE.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 62, 35));
        this.addSlot(new SlotNonRetarded(tile, 1, 35, 17));
        this.addSlot(new SlotTakeOnly(tile, 2, 116, 35));
        this.addSlot(new SlotTakeOnly(tile, 3, 35, 53));
        playerInv(playerInv, 8, 84);
    }
}
