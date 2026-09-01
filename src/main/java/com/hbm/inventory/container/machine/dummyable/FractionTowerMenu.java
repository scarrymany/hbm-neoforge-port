package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineFractionTowerBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** CE has no container (chat inspect). Real tank + fluid-id menu, not a stub. */
public class FractionTowerMenu extends MenuBase<MachineFractionTowerBlockEntity> {

    public FractionTowerMenu(int id, Inventory playerInv, MachineFractionTowerBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_FRACTION_TOWER.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 80, 54));
        playerInv(playerInv, 8, 84);
    }
}
