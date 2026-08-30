package com.hbm.inventory.container.machine.chem;

import com.hbm.blockentity.machine.chem.CentrifugeBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** Ported from CE's {@code ContainerCentrifuge}: input, battery, 4 outputs, 2 upgrade slots. */
public class CentrifugeMenu extends MenuBase<CentrifugeBlockEntity> {

    public CentrifugeMenu(int id, Inventory playerInv, CentrifugeBlockEntity be) {
        super(ChemIsotopeMenus.CENTRIFUGE.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 44, 40));
        this.addSlot(new SlotNonRetarded(tile, 1, 44, 68));
        this.addSlot(new SlotTakeOnly(tile, 2, 116, 21));
        this.addSlot(new SlotTakeOnly(tile, 3, 116, 39));
        this.addSlot(new SlotTakeOnly(tile, 4, 116, 57));
        this.addSlot(new SlotTakeOnly(tile, 5, 116, 75));
        this.addSlot(new SlotNonRetarded(tile, 6, 152, 21));
        this.addSlot(new SlotNonRetarded(tile, 7, 152, 39));

        playerInv(playerInv, 8, 116);
    }
}
