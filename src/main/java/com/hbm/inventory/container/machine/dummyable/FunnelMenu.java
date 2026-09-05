package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineFunnelBlockEntity;
import com.hbm.inventory.container.MenuBase;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code ContainerFunnel}: 9 in / 9 out. Mode via {@code IControlReceiver}, not invented menu buttons.
 */
public class FunnelMenu extends MenuBase<MachineFunnelBlockEntity> {

    public FunnelMenu(int id, Inventory playerInv, MachineFunnelBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_FUNNEL.get(), id, be);
        this.addSlots(tile, 0, 44, 18, 3, 3);
        this.addTakeOnlySlots(tile, 9, 116, 18, 3, 3);
        playerInv(playerInv, 8, 86);
    }
}
