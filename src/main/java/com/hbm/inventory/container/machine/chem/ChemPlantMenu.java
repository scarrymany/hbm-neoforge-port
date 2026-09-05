package com.hbm.inventory.container.machine.chem;

import com.hbm.blockentity.machine.chem.ChemPlantBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code ContainerMachineChemicalPlant.java:36-54}: battery 0@152,81 /
 * blueprint 1@35,126 / upgrades 2-3@152,108 / 152,126 / item in 4@8,99 /
 * item out 7@80,99 / canisters 10-21 / playerInv 8,174.
 */
public class ChemPlantMenu extends MenuBase<ChemPlantBlockEntity> {

    public ChemPlantMenu(int id, Inventory playerInv, ChemPlantBlockEntity be) {
        super(ChemIsotopeMenus.CHEM_PLANT.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, ChemPlantBlockEntity.BATTERY_SLOT, 152, 81));
        this.addSlot(new SlotNonRetarded(tile, ChemPlantBlockEntity.BLUEPRINT_SLOT, 35, 126));
        this.addSlots(tile, ChemPlantBlockEntity.SLOT_UPGRADE_A, 152, 108, 2, 1);
        this.addSlots(tile, ChemPlantBlockEntity.ITEM_IN_START, 8, 99, 1, 3);
        this.addOutputSlots(playerInv.player, tile, ChemPlantBlockEntity.ITEM_OUT_START, 80, 99, 1, 3);

        this.addSlots(tile, 10, 8, 54, 1, 3);
        this.addTakeOnlySlots(tile, 13, 8, 72, 1, 3);
        this.addSlots(tile, 16, 80, 54, 1, 3);
        this.addTakeOnlySlots(tile, 19, 80, 72, 1, 3);

        playerInv(playerInv, 8, 174);
    }
}
