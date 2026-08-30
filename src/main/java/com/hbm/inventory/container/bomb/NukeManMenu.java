package com.hbm.inventory.container.bomb;

import com.hbm.blockentity.bomb.NukeManBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** Menu for {@link NukeManBlockEntity} - 6 slots (igniter, 4x lens, core) in one row. */
public class NukeManMenu extends MenuBase<NukeManBlockEntity> {

    public NukeManMenu(int id, Inventory playerInv, NukeManBlockEntity be) {
        super(NukeCasingMenus.NUKE_MAN.get(), id, be);

        for (int col = 0; col < 6; col++) {
            this.addSlot(new SlotNonRetarded(tile, col, 35 + col * 18, 30));
        }

        playerInv(playerInv, 8, 84);
    }
}
