package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineRotaryFurnaceBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachineRotaryFurnace}: in 8/26/44,18 / id 8,54 / fuel 44,54. */
public class RotaryFurnaceMenu extends MenuBase<MachineRotaryFurnaceBlockEntity> {

    public RotaryFurnaceMenu(int id, Inventory playerInv, MachineRotaryFurnaceBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_ROTARY_FURNACE.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 8, 18));
        this.addSlot(new SlotNonRetarded(tile, 1, 26, 18));
        this.addSlot(new SlotNonRetarded(tile, 2, 44, 18));
        this.addSlot(new SlotNonRetarded(tile, 3, 8, 54));
        this.addSlot(new SlotNonRetarded(tile, 4, 44, 54));
        playerInv(playerInv, 8, 104);
    }
}
