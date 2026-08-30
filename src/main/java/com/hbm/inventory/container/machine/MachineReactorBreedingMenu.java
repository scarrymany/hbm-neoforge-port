package com.hbm.inventory.container.machine;

import com.hbm.blockentity.machine.MachineReactorBreedingBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** Menu for {@link MachineReactorBreedingBlockEntity}, ported from CE's {@code ContainerMachineReactorBreeding}. */
public class MachineReactorBreedingMenu extends MenuBase<MachineReactorBreedingBlockEntity> {

    public MachineReactorBreedingMenu(int id, Inventory playerInv, MachineReactorBreedingBlockEntity be) {
        super(PWRMenus.REACTOR_BREEDING.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 80, 32));
        this.addSlot(new SlotTakeOnly(tile, 1, 80, 61));

        playerInv(playerInv, 8, 84);
    }
}
