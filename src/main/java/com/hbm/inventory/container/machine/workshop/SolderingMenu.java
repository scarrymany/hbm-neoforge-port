package com.hbm.inventory.container.machine.workshop;

import com.hbm.blockentity.machine.workshop.SolderingBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachineSolderingStation}: toppings / pcb / solder + out + battery. */
public class SolderingMenu extends MenuBase<SolderingBlockEntity> {

    public SolderingMenu(int id, Inventory playerInv, SolderingBlockEntity be) {
        super(WorkshopMenus.MACHINE_SOLDERING_STATION.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 26, 18));
        this.addSlot(new SlotNonRetarded(tile, 1, 44, 18));
        this.addSlot(new SlotNonRetarded(tile, 2, 62, 18));
        this.addSlot(new SlotNonRetarded(tile, 3, 26, 36));
        this.addSlot(new SlotNonRetarded(tile, 4, 44, 36));
        this.addSlot(new SlotNonRetarded(tile, 5, 80, 27));
        this.addSlot(new SlotTakeOnly(tile, 6, 134, 27));
        this.addSlot(new SlotNonRetarded(tile, 7, 8, 54));
        playerInv(playerInv, 8, 104);
    }
}
