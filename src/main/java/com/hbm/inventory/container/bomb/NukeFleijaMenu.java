package com.hbm.inventory.container.bomb;

import com.hbm.blockentity.bomb.NukeFleijaBlockEntity;
import com.hbm.inventory.container.MenuBase;
import net.minecraft.world.entity.player.Inventory;

/** Menu for {@link NukeFleijaBlockEntity} - 11 slots (2x igniter, 3x propellant, 6x core) as two rows. */
public class NukeFleijaMenu extends MenuBase<NukeFleijaBlockEntity> {

    public NukeFleijaMenu(int id, Inventory playerInv, NukeFleijaBlockEntity be) {
        super(NukeCasingMenus.NUKE_FLEIJA.get(), id, be);

        addSlots(tile, 0, 26, 20, 1, 5);
        addSlots(tile, 5, 44, 38, 1, 6);

        playerInv(playerInv, 8, 94);
    }
}
