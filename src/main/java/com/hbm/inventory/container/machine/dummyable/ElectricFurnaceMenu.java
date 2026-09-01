package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineElectricFurnaceBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachineElectricFurnace}: slot 1 in / 2 out. Battery + upgrade skipped. */
public class ElectricFurnaceMenu extends MenuBase<MachineElectricFurnaceBlockEntity> {

    public ElectricFurnaceMenu(int id, Inventory playerInv, MachineElectricFurnaceBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_ELECTRIC_FURNACE.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 1, 56, 35));
        this.addSlot(new SlotTakeOnly(tile, 2, 116, 35));
        playerInv(playerInv, 8, 86);
    }
}
