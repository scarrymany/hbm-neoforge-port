package com.hbm.inventory.container.machine;

import com.hbm.blockentity.machine.MachineLargeTurbineBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ported (slot layout trimmed, see {@link MachineLargeTurbineBlockEntity}'s javadoc) from CE's
 * {@code ContainerMachineLargeTurbine}: only the battery-charging slot survives, kept at CE's own
 * pixel position. Purely passive - no buttons.
 */
public class MachineLargeTurbineMenu extends MenuBase<MachineLargeTurbineBlockEntity> {

    public MachineLargeTurbineMenu(int id, Inventory playerInv, MachineLargeTurbineBlockEntity be) {
        super(PowerGenMenus.MACHINE_LARGE_TURBINE.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 98, 53));

        playerInv(playerInv, 8, 84, 142);
    }
}
