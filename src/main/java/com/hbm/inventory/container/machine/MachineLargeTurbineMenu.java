package com.hbm.inventory.container.machine;

import com.hbm.blockentity.machine.MachineLargeTurbineBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code ContainerMachineLargeTurbine.java:37-46}: ID 8,17 / 8,53 / load 44,17 / 44,53 /
 * battery 98,53 / unload 152,17 / 152,53.
 */
public class MachineLargeTurbineMenu extends MenuBase<MachineLargeTurbineBlockEntity> {

    public MachineLargeTurbineMenu(int id, Inventory playerInv, MachineLargeTurbineBlockEntity be) {
        super(PowerGenMenus.MACHINE_LARGE_TURBINE.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 8, 17));
        this.addSlot(new SlotTakeOnly(tile, 1, 8, 53));
        this.addSlot(new SlotNonRetarded(tile, 2, 44, 17));
        this.addSlot(new SlotTakeOnly(tile, 3, 44, 53));
        this.addSlot(new SlotNonRetarded(tile, 4, 98, 53));
        this.addSlot(new SlotNonRetarded(tile, 5, 152, 17));
        this.addSlot(new SlotTakeOnly(tile, 6, 152, 53));

        playerInv(playerInv, 8, 84, 142);
    }
}
