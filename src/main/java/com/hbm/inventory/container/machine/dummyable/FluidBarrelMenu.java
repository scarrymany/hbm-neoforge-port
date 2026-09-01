package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.FluidBarrelBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/** Tank + fluid-id + mode cycle (CE canister slots skipped). */
public class FluidBarrelMenu extends MenuBase<FluidBarrelBlockEntity> {

    public static final int BUTTON_CYCLE = 0;

    public FluidBarrelMenu(int id, Inventory playerInv, FluidBarrelBlockEntity be) {
        super(DummyableProcessMenus.FLUID_BARREL.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 17, 17));
        playerInv(playerInv, 8, 84);
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
