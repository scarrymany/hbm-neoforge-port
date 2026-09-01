package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineBlastFurnaceBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerBlastFurnace}: fuel 80,81 / in 80,27+45 / out 134,72+90. */
public class BlastFurnaceMenu extends MenuBase<MachineBlastFurnaceBlockEntity> {

    public BlastFurnaceMenu(int id, Inventory playerInv, MachineBlastFurnaceBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_BLAST_FURNACE.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 80, 81));
        this.addSlot(new SlotNonRetarded(tile, 1, 80, 27));
        this.addSlot(new SlotNonRetarded(tile, 2, 80, 45));
        this.addSlot(new SlotTakeOnly(tile, 3, 134, 72));
        this.addSlot(new SlotTakeOnly(tile, 4, 134, 90));
        playerInv(playerInv, 8, 140);
    }
}
