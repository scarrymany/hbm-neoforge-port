package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineCatalyticCrackerBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** CE has no container (overlay). Real tank + fluid-id menu, not a stub. */
public class CatalyticCrackerMenu extends MenuBase<MachineCatalyticCrackerBlockEntity> {

    public CatalyticCrackerMenu(int id, Inventory playerInv, MachineCatalyticCrackerBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_CATALYTIC_CRACKER.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 80, 72));
        playerInv(playerInv, 8, 122, 180);
    }
}
