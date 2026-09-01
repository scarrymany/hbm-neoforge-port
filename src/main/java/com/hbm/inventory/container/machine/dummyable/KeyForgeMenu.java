package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineKeyForgeBlockEntity;
import com.hbm.inventory.container.MenuBase;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachineKeyForge}: 3 key slots. */
public class KeyForgeMenu extends MenuBase<MachineKeyForgeBlockEntity> {

    public KeyForgeMenu(int id, Inventory playerInv, MachineKeyForgeBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_KEYFORGE.get(), id, be);
        this.addSlots(tile, 0, 62, 17, 1, 3);
        playerInv(playerInv, 8, 86);
    }
}
