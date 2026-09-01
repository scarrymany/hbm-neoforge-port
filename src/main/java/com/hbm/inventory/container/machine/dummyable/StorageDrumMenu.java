package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.StorageDrumBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerStorageDrum} hexagonal 24-slot grid. */
public class StorageDrumMenu extends MenuBase<StorageDrumBlockEntity> {

    public StorageDrumMenu(int id, Inventory playerInv, StorageDrumBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_STORAGE_DRUM.get(), id, be);
        int slot = 0;
        for (int j = 0; j < 6; j++) {
            for (int i = 0; i < 6; i++) {
                if (i + j > 1 && i + j < 9 && 5 - i + j > 1 && i + 5 - j > 1) {
                    this.addSlot(new SlotNonRetarded(tile, slot++, 35 + i * 18, 24 + j * 18));
                }
            }
        }
        playerInv(playerInv, 8, 155);
    }
}
