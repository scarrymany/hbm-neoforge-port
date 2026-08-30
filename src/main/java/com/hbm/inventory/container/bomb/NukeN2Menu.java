package com.hbm.inventory.container.bomb;

import com.hbm.blockentity.bomb.NukeN2BlockEntity;
import com.hbm.inventory.container.MenuBase;
import net.minecraft.world.entity.player.Inventory;

/** Menu for {@link NukeN2BlockEntity} - 12 single-item charge slots, 4x3 grid. */
public class NukeN2Menu extends MenuBase<NukeN2BlockEntity> {

    public NukeN2Menu(int id, Inventory playerInv, NukeN2BlockEntity be) {
        super(NukeCasingMenus.NUKE_N2.get(), id, be);

        addSlots(tile, 0, 44, 20, 3, 4);

        playerInv(playerInv, 8, 94);
    }
}
