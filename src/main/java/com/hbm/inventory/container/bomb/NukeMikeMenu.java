package com.hbm.inventory.container.bomb;

import com.hbm.blockentity.bomb.NukeMikeBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** Menu for {@link NukeMikeBlockEntity} - 8 slots (4x lens + man core, then mike core/deut/cooling) as two rows of 4. */
public class NukeMikeMenu extends MenuBase<NukeMikeBlockEntity> {

    public NukeMikeMenu(int id, Inventory playerInv, NukeMikeBlockEntity be) {
        super(NukeCasingMenus.NUKE_MIKE.get(), id, be);

        addSlots(tile, 0, 44, 20, 1, 4);
        addSlots(tile, 4, 44, 38, 1, 4);

        playerInv(playerInv, 8, 94);
    }
}
