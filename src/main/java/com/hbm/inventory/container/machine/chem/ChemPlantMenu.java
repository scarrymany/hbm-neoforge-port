package com.hbm.inventory.container.machine.chem;

import com.hbm.blockentity.machine.chem.ChemPlantBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/**
 * Item in/out/battery keep the port's 0-6 layout. Canister columns Exact CE
 * {@code ContainerMachineChemicalPlant.java:47-52}: load 10@8,54 / empty 13@8,72 /
 * unload 16@80,54 / filled 19@80,72.
 */
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

        this.addSlots(tile, 10, 8, 54, 1, 3);
        this.addTakeOnlySlots(tile, 13, 8, 72, 1, 3);
        this.addSlots(tile, 16, 80, 54, 1, 3);
        this.addTakeOnlySlots(tile, 19, 80, 72, 1, 3);

        playerInv(playerInv, 8, 116);
    }
}
