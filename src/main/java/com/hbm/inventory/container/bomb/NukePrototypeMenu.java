package com.hbm.inventory.container.bomb;

import com.hbm.blockentity.bomb.NukePrototypeBlockEntity;
import com.hbm.inventory.container.MenuBase;
import net.minecraft.world.entity.player.Inventory;

/** Menu for {@link NukePrototypeBlockEntity} - 14 single-item slots as two rows of 7. */
public class NukePrototypeMenu extends MenuBase<NukePrototypeBlockEntity> {

    public NukePrototypeMenu(int id, Inventory playerInv, NukePrototypeBlockEntity be) {
        super(NukeCasingMenus.NUKE_PROTOTYPE.get(), id, be);

        addSlots(tile, 0, 26, 20, 1, 7);
        addSlots(tile, 7, 26, 38, 1, 7);

        playerInv(playerInv, 8, 94);
    }
}
