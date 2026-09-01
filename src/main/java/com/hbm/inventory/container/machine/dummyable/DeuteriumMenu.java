package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.DeuteriumExtractorBlockEntity;
import com.hbm.inventory.container.MenuBase;
import net.minecraft.world.entity.player.Inventory;

/** Dual-tank + energy inspect (CE deuterium is overlay-only / 0 slots). */
public class DeuteriumMenu extends MenuBase<DeuteriumExtractorBlockEntity> {

    public DeuteriumMenu(int id, Inventory playerInv, DeuteriumExtractorBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_DEUTERIUM.get(), id, be);
        playerInv(playerInv, 8, 84);
    }
}
