package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineDiFurnaceBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerDiFurnace}: 2 in / fuel / out. */
public class DiFurnaceMenu extends MenuBase<MachineDiFurnaceBlockEntity> {

    public DiFurnaceMenu(int id, Inventory playerInv, MachineDiFurnaceBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_DIFURNACE.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 80, 18));
        this.addSlot(new SlotNonRetarded(tile, 1, 80, 54));
        this.addSlot(new SlotNonRetarded(tile, 2, 8, 36));
        this.addSlot(new SlotTakeOnly(tile, 3, 134, 36));
        playerInv(playerInv, 8, 86);
    }
}
