package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineTeleLinkerBlockEntity;
import com.hbm.inventory.container.MenuBase;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachineTeleLinker}: 3 chip slots at 36px spacing. */
public class TeleLinkerMenu extends MenuBase<MachineTeleLinkerBlockEntity> {

    public TeleLinkerMenu(int id, Inventory playerInv, MachineTeleLinkerBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_TELELINKER.get(), id, be);
        this.addSlots(tile, 0, 44, 35, 1, 3, 36);
        playerInv(playerInv, 8, 84);
    }
}
