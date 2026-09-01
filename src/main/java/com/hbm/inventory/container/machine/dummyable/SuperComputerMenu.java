package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineSuperComputerBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachineSuperComputer}. */
public class SuperComputerMenu extends MenuBase<MachineSuperComputerBlockEntity> {

    public SuperComputerMenu(int id, Inventory playerInv, MachineSuperComputerBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_SUPERCOMPUTER.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 152, 81));
        this.addSlot(new SlotNonRetarded(tile, 1, 35, 80));
        this.addSlot(new SlotNonRetarded(tile, 2, 8, 27));
        this.addSlot(new SlotNonRetarded(tile, 3, 8, 45));
        this.addSlot(new SlotNonRetarded(tile, 4, 8, 63));
        this.addSlot(new SlotTakeOnly(tile, 5, 80, 27));
        this.addSlot(new SlotTakeOnly(tile, 6, 80, 45));
        this.addSlot(new SlotTakeOnly(tile, 7, 80, 63));
        playerInv(playerInv, 8, 129);
    }
}
