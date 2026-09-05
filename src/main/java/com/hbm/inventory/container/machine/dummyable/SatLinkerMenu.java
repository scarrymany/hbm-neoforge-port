package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineSatLinkerBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachineSatLinker}: 44,36 / 80,36 / 116,36; player y=104/162. */
public class SatLinkerMenu extends MenuBase<MachineSatLinkerBlockEntity> {

    public SatLinkerMenu(int id, Inventory playerInv, MachineSatLinkerBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_SATLINKER.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 44, 36));
        this.addSlot(new SlotNonRetarded(tile, 1, 80, 36));
        this.addSlot(new SlotNonRetarded(tile, 2, 116, 36));
        playerInv(playerInv, 8, 104, 162);
    }
}
