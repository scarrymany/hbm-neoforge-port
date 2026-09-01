package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.FilingCabinetBlockEntity;
import com.hbm.inventory.container.MenuBase;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerFileCabinet}: 2×4 with 36px row gap. */
public class FileCabinetMenu extends MenuBase<FilingCabinetBlockEntity> {

    public FileCabinetMenu(int id, Inventory playerInv, FilingCabinetBlockEntity be) {
        super(DummyableProcessMenus.FILING_CABINET.get(), id, be);
        this.addSlots(tile, 0, 53, 18, 1, 4);
        this.addSlots(tile, 4, 53, 54, 1, 4);
        playerInv(playerInv, 8, 88, 146);
    }
}
