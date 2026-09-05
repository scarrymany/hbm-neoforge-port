package com.hbm.inventory.container.machine.workshop;

import com.hbm.blockentity.machine.workshop.AmmoPressBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** Exact CE {@code ContainerMachineAmmoPress}: 3×3 at 116,18 / out 134,72 / player 8,118+176. */
public class AmmoPressMenu extends MenuBase<AmmoPressBlockEntity> {

    public AmmoPressMenu(int id, Inventory playerInv, AmmoPressBlockEntity be) {
        super(WorkshopMenus.MACHINE_AMMO_PRESS.get(), id, be);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(new SlotNonRetarded(tile, i * 3 + j, 116 + j * 18, 18 + i * 18));
            }
        }
        this.addSlot(new SlotTakeOnly(tile, 9, 134, 72));
        playerInv(playerInv, 8, 118, 176);
    }
}
