package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineIntakeBlockEntity;
import com.hbm.inventory.container.MenuBase;
import net.minecraft.world.entity.player.Inventory;

/** Tank inspect (CE intake is overlay-only). */
public class IntakeMenu extends MenuBase<MachineIntakeBlockEntity> {

    public IntakeMenu(int id, Inventory playerInv, MachineIntakeBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_INTAKE.get(), id, be);
        playerInv(playerInv, 8, 84);
    }
}
