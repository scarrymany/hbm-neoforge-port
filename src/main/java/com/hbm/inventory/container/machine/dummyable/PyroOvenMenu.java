package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachinePyroOvenBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerPyroOven}: battery 152,72 / in 35,45 / out 89,45 / id 8,72 / upgrades 71,72 + 89,72. */
public class PyroOvenMenu extends MenuBase<MachinePyroOvenBlockEntity> {

    public PyroOvenMenu(int id, Inventory playerInv, MachinePyroOvenBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_PYROOVEN.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 152, 72));
        this.addSlot(new SlotNonRetarded(tile, 1, 35, 45));
        this.addSlot(new SlotTakeOnly(tile, 2, 89, 45));
        this.addSlot(new SlotNonRetarded(tile, 3, 8, 72));
        this.addSlot(new SlotNonRetarded(tile, 4, 71, 72));
        this.addSlot(new SlotNonRetarded(tile, 5, 89, 72));
        playerInv(playerInv, 8, 122, 180);
    }
}
