package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachinePressBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachinePress}: fuel 26,53 / stamp 80,17 / in 80,53 / out 140,35 / storage 8,84. */
public class PressMenu extends MenuBase<MachinePressBlockEntity> {

    public PressMenu(int id, Inventory playerInv, MachinePressBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_PRESS.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 26, 53));
        this.addSlot(new SlotNonRetarded(tile, 1, 80, 17));
        this.addSlot(new SlotNonRetarded(tile, 2, 80, 53));
        this.addSlot(new SlotTakeOnly(tile, 3, 140, 35));
        for (int i = 0; i < 9; i++) {
            this.addSlot(new SlotNonRetarded(tile, 4 + i, 8 + i * 18, 84));
        }
        playerInv(playerInv, 8, 132);
    }
}
