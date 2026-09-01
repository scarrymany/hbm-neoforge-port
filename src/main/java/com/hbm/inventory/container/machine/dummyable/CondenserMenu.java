package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.CondenserBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** Tank + fluid-id menu (CE condenser / towers are overlay-only). */
public class CondenserMenu extends MenuBase<CondenserBlockEntity> {

    public CondenserMenu(int id, Inventory playerInv, CondenserBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_CONDENSER.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 80, 54));
        playerInv(playerInv, 8, 84);
    }
}
