package com.hbm.inventory.container.machine.chem;

import com.hbm.blockentity.machine.chem.ElectrolyserBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** Ported (fluid side only, see {@link ElectrolyserBlockEntity}'s javadoc) from CE's {@code ContainerElectrolyserFluid}: battery, 2 upgrades, 3 byproduct slots. */
public class ElectrolyserMenu extends MenuBase<ElectrolyserBlockEntity> {

    public ElectrolyserMenu(int id, Inventory playerInv, ElectrolyserBlockEntity be) {
        super(ChemIsotopeMenus.ELECTROLYSER.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 8, 21));
        this.addSlot(new SlotNonRetarded(tile, 1, 8, 39));
        this.addSlot(new SlotNonRetarded(tile, 2, 26, 39));
        this.addSlot(new SlotTakeOnly(tile, 3, 116, 21));
        this.addSlot(new SlotTakeOnly(tile, 4, 116, 39));
        this.addSlot(new SlotTakeOnly(tile, 5, 116, 57));

        playerInv(playerInv, 8, 116);
    }
}
