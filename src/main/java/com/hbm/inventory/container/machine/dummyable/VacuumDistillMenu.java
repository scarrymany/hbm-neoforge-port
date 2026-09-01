package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineVacuumDistillBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachineVacuumDistill} 10-slot layout (input canister already gone in CE). */
public class VacuumDistillMenu extends MenuBase<MachineVacuumDistillBlockEntity> {

    public VacuumDistillMenu(int id, Inventory playerInv, MachineVacuumDistillBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_VACUUM_DISTILL.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 26, 90));
        this.addSlot(new SlotNonRetarded(tile, 1, 80, 90));
        this.addSlot(new SlotTakeOnly(tile, 2, 80, 108));
        this.addSlot(new SlotNonRetarded(tile, 3, 98, 90));
        this.addSlot(new SlotTakeOnly(tile, 4, 98, 108));
        this.addSlot(new SlotNonRetarded(tile, 5, 116, 90));
        this.addSlot(new SlotTakeOnly(tile, 6, 116, 108));
        this.addSlot(new SlotNonRetarded(tile, 7, 134, 90));
        this.addSlot(new SlotTakeOnly(tile, 8, 134, 108));
        this.addSlot(new SlotNonRetarded(tile, 9, 26, 108));
        playerInv(playerInv, 8, 156, 214);
    }
}
