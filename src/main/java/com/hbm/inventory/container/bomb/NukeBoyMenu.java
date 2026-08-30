package com.hbm.inventory.container.bomb;

import com.hbm.blockentity.bomb.NukeBoyBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** Menu for {@link NukeBoyBlockEntity} - 5 slots (shielding/target/bullet/propellant/igniter) in one row. */
public class NukeBoyMenu extends MenuBase<NukeBoyBlockEntity> {

    public NukeBoyMenu(int id, Inventory playerInv, NukeBoyBlockEntity be) {
        super(NukeCasingMenus.NUKE_BOY.get(), id, be);

        for (int col = 0; col < 5; col++) {
            this.addSlot(new SlotNonRetarded(tile, col, 44 + col * 18, 30));
        }

        playerInv(playerInv, 8, 84);
    }
}
