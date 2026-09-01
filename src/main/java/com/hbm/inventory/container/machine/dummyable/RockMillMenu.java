package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineRockMillBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachineRockMill}: battery 152,91 / schematic 35,90 / in 8,27 / out 80,27. */
public class RockMillMenu extends MenuBase<MachineRockMillBlockEntity> {

    public RockMillMenu(int id, Inventory playerInv, MachineRockMillBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_ROCK_MILL.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 152, 91));
        this.addSlot(new SlotNonRetarded(tile, 1, 35, 90));
        addSlots(tile, 2, 8, 27, 1, 3);
        addOutputSlots(playerInv.player, tile, 5, 80, 27, 1, 3);
        playerInv(playerInv, 8, 138);
    }
}
