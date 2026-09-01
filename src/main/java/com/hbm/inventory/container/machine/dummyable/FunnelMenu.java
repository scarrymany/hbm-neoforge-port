package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineFunnelBlockEntity;
import com.hbm.inventory.container.MenuBase;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/** CE {@code ContainerMachineFunnel}: 9 in / 9 out + mode cycle. */
public class FunnelMenu extends MenuBase<MachineFunnelBlockEntity> {

    public static final int BUTTON_CYCLE = 0;

    public FunnelMenu(int id, Inventory playerInv, MachineFunnelBlockEntity be) {
        super(DummyableProcessMenus.MACHINE_FUNNEL.get(), id, be);
        this.addSlots(tile, 0, 44, 18, 3, 3);
        this.addTakeOnlySlots(tile, 9, 116, 18, 3, 3);
        playerInv(playerInv, 8, 86);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_CYCLE) {
            be.cycleMode();
            return true;
        }
        return super.clickMenuButton(player, id);
    }
}
