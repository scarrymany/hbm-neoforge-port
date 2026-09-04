package com.hbm.inventory.container.machine.workshop;

import com.hbm.blockentity.machine.workshop.ArcWelderBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code ContainerMachineArcWelder.java:35-43}: 3 in + out + battery + fluid ID.
 * Upgrades 6-7 skipped. {@code setType(5)} Exact CE {@code TileEntityMachineArcWelder.java:121}.
 */
public class ArcWelderMenu extends MenuBase<ArcWelderBlockEntity> {

    public ArcWelderMenu(int id, Inventory playerInv, ArcWelderBlockEntity be) {
        super(WorkshopMenus.MACHINE_ARC_WELDER.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 17, 36));
        this.addSlot(new SlotNonRetarded(tile, 1, 35, 36));
        this.addSlot(new SlotNonRetarded(tile, 2, 53, 36));
        this.addSlot(new SlotTakeOnly(tile, 3, 107, 36));
        this.addSlot(new SlotNonRetarded(tile, 4, 152, 72));
        this.addSlot(new SlotNonRetarded(tile, 5, 17, 63));
        playerInv(playerInv, 8, 122, 180);
    }
}
