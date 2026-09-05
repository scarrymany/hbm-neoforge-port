package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.FilingCabinetBlockEntity;
import com.hbm.inventory.container.MenuBase;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/** CE {@code ContainerFileCabinet}: 2×4 with 36px row gap. Open/close Exact CE {@code :53-60}. */
public class FileCabinetMenu extends MenuBase<FilingCabinetBlockEntity> {

    public FileCabinetMenu(int id, Inventory playerInv, FilingCabinetBlockEntity be) {
        super(DummyableProcessMenus.FILING_CABINET.get(), id, be);
        this.addSlots(tile, 0, 53, 18, 1, 4);
        this.addSlots(tile, 4, 53, 54, 1, 4);
        playerInv(playerInv, 8, 88, 146);
        if (!playerInv.player.level().isClientSide()) {
            be.openInventory(playerInv.player);
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            be.closeInventory(player);
        }
    }
}
