package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineSirenBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachineSiren} cassette slot 80,36. */
public class SirenMenu extends MenuBase<MachineSirenBlockEntity> {

    public SirenMenu(int id, Inventory playerInv, MachineSirenBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_SIREN.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 80, 36));
        playerInv(playerInv, 8, 84);
    }
}
