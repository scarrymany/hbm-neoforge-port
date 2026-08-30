package com.hbm.inventory.container.bomb;

import com.hbm.blockentity.bomb.NukeTsarBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** Menu for {@link NukeTsarBlockEntity} - 6 slots (4x lens, man core, tsar core) in one row. */
public class NukeTsarMenu extends MenuBase<NukeTsarBlockEntity> {

    public NukeTsarMenu(int id, Inventory playerInv, NukeTsarBlockEntity be) {
        super(NukeCasingMenus.NUKE_TSAR.get(), id, be);

        for (int col = 0; col < 6; col++) {
            this.addSlot(new SlotNonRetarded(tile, col, 35 + col * 18, 30));
        }

        playerInv(playerInv, 8, 84);
    }
}
