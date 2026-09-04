package com.hbm.inventory.container.machine.oil;

import com.hbm.blockentity.machine.oil.MachineRefineryBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code ContainerMachineRefinery.java:41-66}: battery 186,72 / load 8,99 + 8,119 takeOnly /
 * heavy 86,99 + 86,119 / naphtha 106,99 + 106,119 / light 126,99 + 126,119 / petroleum 146,99 + 146,119 /
 * sulfur 58,119 takeOnly / ID 186,106.
 * {@code setType(12)} / {@code loadTank(1,2)} / {@code unloadTank} 3-10 Exact CE
 * {@code TileEntityMachineRefinery.java:137-145}.
 */
public class MachineRefineryMenu extends MenuBase<MachineRefineryBlockEntity> {

    public MachineRefineryMenu(int id, Inventory playerInv, MachineRefineryBlockEntity be) {
        super(OilChainMenus.MACHINE_REFINERY.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 186, 72));
        this.addSlot(new SlotNonRetarded(tile, 1, 8, 99));
        this.addSlot(new SlotTakeOnly(tile, 2, 8, 119));
        this.addSlot(new SlotNonRetarded(tile, 3, 86, 99));
        this.addSlot(new SlotTakeOnly(tile, 4, 86, 119));
        this.addSlot(new SlotNonRetarded(tile, 5, 106, 99));
        this.addSlot(new SlotTakeOnly(tile, 6, 106, 119));
        this.addSlot(new SlotNonRetarded(tile, 7, 126, 99));
        this.addSlot(new SlotTakeOnly(tile, 8, 126, 119));
        this.addSlot(new SlotNonRetarded(tile, 9, 146, 99));
        this.addSlot(new SlotTakeOnly(tile, 10, 146, 119));
        this.addSlot(new SlotTakeOnly(tile, 11, 58, 119));
        this.addSlot(new SlotNonRetarded(tile, 12, 186, 106));

        playerInv(playerInv, 8, 150, 208);
    }
}
