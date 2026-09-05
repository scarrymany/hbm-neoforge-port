package com.hbm.inventory.container.machine;

import com.hbm.blockentity.machine.MachineReactorBreedingBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code ContainerMachineReactorBreeding.java:29-30}: in 35,35 / out 125,35.
 * Player inv 8,84. Existing {@code gui_breeder.png} — not invent.
 */
public class MachineReactorBreedingMenu extends MenuBase<MachineReactorBreedingBlockEntity> {

    public MachineReactorBreedingMenu(int id, Inventory playerInv, MachineReactorBreedingBlockEntity be) {
        super(PWRMenus.REACTOR_BREEDING.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 35, 35));
        this.addSlot(new SlotTakeOnly(tile, 1, 125, 35));

        playerInv(playerInv, 8, 84);
    }
}
