package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineEPressBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachineEPress}: battery / stamp / in / out / upgrade. */
public class EPressMenu extends MenuBase<MachineEPressBlockEntity> {

    public EPressMenu(int id, Inventory playerInv, MachineEPressBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_EPRESS.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 152, 54));
        this.addSlot(new SlotNonRetarded(tile, 1, 19, 15));
        this.addSlot(new SlotNonRetarded(tile, 2, 19, 51));
        this.addSlot(new SlotTakeOnly(tile, 3, 79, 33));
        this.addSlot(new SlotNonRetarded(tile, 4, 111, 32));
        playerInv(playerInv, 8, 104, 162);
    }
}
