package com.hbm.inventory.container.machine.workshop;

import com.hbm.blockentity.machine.workshop.AmmoPressBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachineAmmoPress}: 3×3 + output. */
public class AmmoPressMenu extends MenuBase<AmmoPressBlockEntity> {

    public AmmoPressMenu(int id, Inventory playerInv, AmmoPressBlockEntity be) {
        super(WorkshopMenus.MACHINE_AMMO_PRESS.get(), id, be);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new SlotNonRetarded(tile, row * 3 + col, 44 + col * 18, 17 + row * 18));
            }
        }
        this.addSlot(new SlotTakeOnly(tile, 9, 134, 35));
        playerInv(playerInv, 8, 104);
    }
}
