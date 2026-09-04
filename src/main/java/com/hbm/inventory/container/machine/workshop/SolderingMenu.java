package com.hbm.inventory.container.machine.workshop;

import com.hbm.blockentity.machine.workshop.SolderingBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code ContainerMachineSolderingStation.java:29-38}: toppings / pcb / solder + out +
 * battery + fluid ID. Upgrades 9-10 skipped. {@code setType(8)} Exact CE
 * {@code TileEntityMachineSolderingStation.java:123}.
 */
public class SolderingMenu extends MenuBase<SolderingBlockEntity> {

    public SolderingMenu(int id, Inventory playerInv, SolderingBlockEntity be) {
        super(WorkshopMenus.MACHINE_SOLDERING_STATION.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 17, 18));
        this.addSlot(new SlotNonRetarded(tile, 1, 35, 18));
        this.addSlot(new SlotNonRetarded(tile, 2, 53, 18));
        this.addSlot(new SlotNonRetarded(tile, 3, 17, 36));
        this.addSlot(new SlotNonRetarded(tile, 4, 35, 36));
        this.addSlot(new SlotNonRetarded(tile, 5, 53, 36));
        this.addSlot(new SlotTakeOnly(tile, 6, 107, 27));
        this.addSlot(new SlotNonRetarded(tile, 7, 152, 72));
        this.addSlot(new SlotNonRetarded(tile, 8, 17, 63));
        playerInv(playerInv, 8, 122, 180);
    }
}
