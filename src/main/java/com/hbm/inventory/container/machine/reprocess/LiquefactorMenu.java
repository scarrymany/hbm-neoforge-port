package com.hbm.inventory.container.machine.reprocess;

import com.hbm.blockentity.machine.reprocess.LiquefactorBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code ContainerLiquefactor.java:35-40}: item in + battery + upgrades 2-3.
 */
public class LiquefactorMenu extends MenuBase<LiquefactorBlockEntity> {

    public LiquefactorMenu(int id, Inventory playerInv, LiquefactorBlockEntity be) {
        super(ReprocessMenus.MACHINE_LIQUEFACTOR.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 35, 54));
        this.addSlot(new SlotNonRetarded(tile, 1, 134, 72));
        this.addSlot(new SlotNonRetarded(tile, 2, 98, 36));
        this.addSlot(new SlotNonRetarded(tile, 3, 98, 54));

        playerInv(playerInv, 8, 122, 180);
    }
}
