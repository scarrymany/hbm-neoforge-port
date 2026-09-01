package com.hbm.inventory.container.machine.workshop;

import com.hbm.blockentity.machine.workshop.ArcWelderBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachineArcWelder}: 3 in + out + battery. Upgrades not ported. */
public class ArcWelderMenu extends MenuBase<ArcWelderBlockEntity> {

    public ArcWelderMenu(int id, Inventory playerInv, ArcWelderBlockEntity be) {
        super(WorkshopMenus.MACHINE_ARC_WELDER.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 44, 18));
        this.addSlot(new SlotNonRetarded(tile, 1, 62, 18));
        this.addSlot(new SlotNonRetarded(tile, 2, 80, 18));
        this.addSlot(new SlotTakeOnly(tile, 3, 134, 18));
        this.addSlot(new SlotNonRetarded(tile, 4, 8, 36));
        playerInv(playerInv, 8, 104);
    }
}
