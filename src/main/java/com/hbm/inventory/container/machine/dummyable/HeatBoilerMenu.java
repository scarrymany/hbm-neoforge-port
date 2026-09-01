package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.HeatBoilerBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** Tank + fluid-id menu (CE boilers are overlay-only). */
public class HeatBoilerMenu extends MenuBase<HeatBoilerBlockEntity> {

    public HeatBoilerMenu(int id, Inventory playerInv, HeatBoilerBlockEntity be) {
        super(DummyableProcessMenus.HEAT_BOILER.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 80, 54));
        playerInv(playerInv, 8, 84);
    }
}
