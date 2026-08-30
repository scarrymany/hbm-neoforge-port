package com.hbm.inventory.container.machine.chem;

import com.hbm.blockentity.machine.chem.CyclotronBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** Ported from CE's {@code ContainerMachineCyclotron}: 3 catalyst, 3 target, 3 output, battery, 2 upgrade slots. */
public class CyclotronMenu extends MenuBase<CyclotronBlockEntity> {

    public CyclotronMenu(int id, Inventory playerInv, CyclotronBlockEntity be) {
        super(ChemIsotopeMenus.CYCLOTRON.get(), id, be);

        for (int i = 0; i < 3; i++) {
            this.addSlot(new SlotNonRetarded(tile, i, 44, 21 + i * 18));
            this.addSlot(new SlotNonRetarded(tile, 3 + i, 80, 21 + i * 18));
            this.addSlot(new SlotTakeOnly(tile, 6 + i, 134, 21 + i * 18));
        }
        this.addSlot(new SlotNonRetarded(tile, 9, 8, 80));
        this.addSlot(new SlotNonRetarded(tile, 10, 8, 98));
        this.addSlot(new SlotNonRetarded(tile, 11, 26, 98));

        playerInv(playerInv, 8, 130);
    }
}
