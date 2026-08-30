package com.hbm.inventory.container.bomb;

import com.hbm.blockentity.bomb.NukeCustomBlockEntity;
import com.hbm.inventory.container.MenuBase;
import net.minecraft.world.entity.player.Inventory;

/** Menu for {@link NukeCustomBlockEntity} - 27 free slots as a 9x3 grid, matching a triple-chest layout. */
public class NukeCustomMenu extends MenuBase<NukeCustomBlockEntity> {

    public NukeCustomMenu(int id, Inventory playerInv, NukeCustomBlockEntity be) {
        super(NukeCasingMenus.NUKE_CUSTOM.get(), id, be);

        addSlots(tile, 0, 8, 18, 3, 9);

        playerInv(playerInv, 8, 104);
    }
}
