package com.hbm.inventory.container.machine.chem;

import com.hbm.blockentity.machine.chem.ChemPlantBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** Ported (auto-recognition, see {@link ChemPlantBlockEntity}'s javadoc) from CE's {@code ContainerMachineChemicalPlant}: 3 item in, 3 item out, battery. */
public class ChemPlantMenu extends MenuBase<ChemPlantBlockEntity> {

    public ChemPlantMenu(int id, Inventory playerInv, ChemPlantBlockEntity be) {
        super(ChemIsotopeMenus.CHEM_PLANT.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 44, 21));
        this.addSlot(new SlotNonRetarded(tile, 1, 44, 39));
        this.addSlot(new SlotNonRetarded(tile, 2, 44, 57));
        this.addSlot(new SlotTakeOnly(tile, 3, 116, 21));
        this.addSlot(new SlotTakeOnly(tile, 4, 116, 39));
        this.addSlot(new SlotTakeOnly(tile, 5, 116, 57));
        this.addSlot(new SlotNonRetarded(tile, 6, 8, 21));

        playerInv(playerInv, 8, 116);
    }
}
