package com.hbm.inventory.container.machine.chem;

import com.hbm.blockentity.machine.chem.SilexBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** Ported (renumbered, see {@link SilexBlockEntity}'s javadoc) from CE's {@code ContainerSILEX}: input, output, 6-slot queue. */
public class SilexMenu extends MenuBase<SilexBlockEntity> {

    public SilexMenu(int id, Inventory playerInv, SilexBlockEntity be) {
        super(ChemIsotopeMenus.SILEX.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 44, 40));
        this.addSlot(new SlotTakeOnly(tile, 1, 98, 40));

        for (int i = 0; i < 6; i++) {
            this.addSlot(new SlotTakeOnly(tile, 2 + i, 116 + i * 18, 40));
        }

        playerInv(playerInv, 8, 116);
    }
}
