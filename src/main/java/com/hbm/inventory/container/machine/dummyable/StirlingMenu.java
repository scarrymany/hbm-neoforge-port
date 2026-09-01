package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineStirlingBlockEntity;
import com.hbm.inventory.container.MenuBase;
import net.minecraft.world.entity.player.Inventory;

/** CE overlay-only Stirling → live heat/power menu. */
public class StirlingMenu extends MenuBase<MachineStirlingBlockEntity> {

    public StirlingMenu(int id, Inventory playerInv, MachineStirlingBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_STIRLING.get(), id, be);
        playerInv(playerInv, 8, 86);
    }
}
