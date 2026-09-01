package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineRtgFurnaceBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerRtgFurnace}: in / 3 pellets / out. */
public class RtgFurnaceMenu extends MenuBase<MachineRtgFurnaceBlockEntity> {

    public RtgFurnaceMenu(int id, Inventory playerInv, MachineRtgFurnaceBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_RTG_FURNACE.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 56, 17));
        this.addSlot(new SlotNonRetarded(tile, 1, 38, 53));
        this.addSlot(new SlotNonRetarded(tile, 2, 56, 53));
        this.addSlot(new SlotNonRetarded(tile, 3, 74, 53));
        this.addSlot(new SlotTakeOnly(tile, 4, 116, 35));
        playerInv(playerInv, 8, 86);
    }
}
