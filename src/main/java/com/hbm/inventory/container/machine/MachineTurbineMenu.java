package com.hbm.inventory.container.machine;

import com.hbm.blockentity.machine.MachineTurbineBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ported (slot layout trimmed, see {@link MachineTurbineBlockEntity}'s javadoc) from CE's
 * {@code ContainerMachineTurbine}: only the battery-charging slot survives, kept at CE's own
 * pixel position (slot 4 in CE's 7-slot inventory). Purely passive - no buttons.
 */
public class MachineTurbineMenu extends MenuBase<MachineTurbineBlockEntity> {

    public MachineTurbineMenu(int id, Inventory playerInv, MachineTurbineBlockEntity be) {
        super(PowerGenMenus.MACHINE_TURBINE.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 98, 53));

        playerInv(playerInv, 8, 84, 142);
    }
}
