package com.hbm.inventory.container.bomb;

import com.hbm.blockentity.bomb.NukeGadgetBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** Menu for {@link NukeGadgetBlockEntity} - 6 slots (wireing, 4x lens, core) in one row. */
public class NukeGadgetMenu extends MenuBase<NukeGadgetBlockEntity> {

    public NukeGadgetMenu(int id, Inventory playerInv, NukeGadgetBlockEntity be) {
        super(NukeCasingMenus.NUKE_GADGET.get(), id, be);

        for (int col = 0; col < 6; col++) {
            this.addSlot(new SlotNonRetarded(tile, col, 35 + col * 18, 30));
        }

        playerInv(playerInv, 8, 84);
    }
}
