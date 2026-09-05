package com.hbm.inventory.container.machine.reprocess;

import com.hbm.blockentity.machine.reprocess.SolidifierBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code ContainerSolidifier.java:34-41}: out + battery + upgrades 2-3 + fluid ID.
 * {@code setType(4)} Exact CE {@code TileEntityMachineSolidifier.java:89}.
 */
public class SolidifierMenu extends MenuBase<SolidifierBlockEntity> {

    public SolidifierMenu(int id, Inventory playerInv, SolidifierBlockEntity be) {
        super(ReprocessMenus.MACHINE_SOLIDIFIER.get(), id, be);

        this.addSlot(new SlotTakeOnly(tile, 0, 71, 45));
        this.addSlot(new SlotNonRetarded(tile, 1, 134, 72));
        this.addSlot(new SlotNonRetarded(tile, 2, 98, 36));
        this.addSlot(new SlotNonRetarded(tile, 3, 98, 54));
        this.addSlot(new SlotNonRetarded(tile, 4, 71, 72));

        playerInv(playerInv, 8, 122, 180);
    }
}
