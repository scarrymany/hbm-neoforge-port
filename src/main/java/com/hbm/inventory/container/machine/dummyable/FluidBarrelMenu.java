package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.FluidBarrelBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/** Exact CE {@code ContainerBarrel.java:40-45}: ID 8,17 / 8,53 / load 35,17 / 35,53 / unload 125,17 / 125,53. */
public class FluidBarrelMenu extends MenuBase<FluidBarrelBlockEntity> {

    public static final int BUTTON_CYCLE = 0;

    public FluidBarrelMenu(int id, Inventory playerInv, FluidBarrelBlockEntity be) {
        super(DummyableProcessMenus.FLUID_BARREL.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 8, 17));
        this.addSlot(new SlotTakeOnly(tile, 1, 8, 53));
        this.addSlot(new SlotNonRetarded(tile, 2, 35, 17));
        this.addSlot(new SlotTakeOnly(tile, 3, 35, 53));
        this.addSlot(new SlotNonRetarded(tile, 4, 125, 17));
        this.addSlot(new SlotTakeOnly(tile, 5, 125, 53));
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
