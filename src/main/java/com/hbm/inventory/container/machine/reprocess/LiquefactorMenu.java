package com.hbm.inventory.container.machine.reprocess;

import com.hbm.blockentity.machine.reprocess.LiquefactorBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerLiquefactor}: item in + battery. Upgrades not ported. */
public class LiquefactorMenu extends MenuBase<LiquefactorBlockEntity> {

    public LiquefactorMenu(int id, Inventory playerInv, LiquefactorBlockEntity be) {
        super(ReprocessMenus.MACHINE_LIQUEFACTOR.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 44, 39));
        this.addSlot(new SlotNonRetarded(tile, 1, 8, 21));

        playerInv(playerInv, 8, 104);
    }
}
