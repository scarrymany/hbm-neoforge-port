package com.hbm.inventory.container.machine.chem;

import com.hbm.blockentity.machine.chem.GasCentrifugeBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** Ported (renumbered, see {@link GasCentrifugeBlockEntity}'s javadoc) from CE's {@code ContainerMachineGasCent}. */
public class GasCentrifugeMenu extends MenuBase<GasCentrifugeBlockEntity> {

    public GasCentrifugeMenu(int id, Inventory playerInv, GasCentrifugeBlockEntity be) {
        super(ChemIsotopeMenus.GAS_CENTRIFUGE.get(), id, be);

        this.addSlot(new SlotTakeOnly(tile, 0, 44, 21));
        this.addSlot(new SlotTakeOnly(tile, 1, 44, 39));
        this.addSlot(new SlotTakeOnly(tile, 2, 44, 57));
        this.addSlot(new SlotTakeOnly(tile, 3, 44, 75));
        this.addSlot(new SlotNonRetarded(tile, 4, 116, 40));
        this.addSlot(new SlotNonRetarded(tile, 5, 152, 40));

        playerInv(playerInv, 8, 116);
    }
}
