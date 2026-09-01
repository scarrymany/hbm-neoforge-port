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

        this.addSlot(new SlotNonRetarded(tile, 0, 44, 57));
        this.addSlot(new SlotNonRetarded(tile, 1, 8, 57));
        this.addSlot(new SlotTakeOnly(tile, 2, 70, 57));
        this.addSlot(new SlotTakeOnly(tile, 3, 90, 57));
        this.addSlot(new SlotTakeOnly(tile, 4, 110, 57));
        this.addSlot(new SlotTakeOnly(tile, 5, 130, 57));
        this.addSlot(new SlotNonRetarded(tile, 6, 156, 31));
        this.addSlot(new SlotNonRetarded(tile, 7, 156, 49));

        playerInv(playerInv, 11, 107);
    }
}
