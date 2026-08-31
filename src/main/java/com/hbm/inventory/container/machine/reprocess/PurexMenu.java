package com.hbm.inventory.container.machine.reprocess;

import com.hbm.blockentity.machine.reprocess.PurexBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerMachinePUREX}: 3 in, 6 out, battery. Auto-detect, no blueprint slot. */
public class PurexMenu extends MenuBase<PurexBlockEntity> {

    public PurexMenu(int id, Inventory playerInv, PurexBlockEntity be) {
        super(ReprocessMenus.MACHINE_PUREX.get(), id, be);

        this.addSlot(new SlotNonRetarded(tile, 0, 44, 21));
        this.addSlot(new SlotNonRetarded(tile, 1, 44, 39));
        this.addSlot(new SlotNonRetarded(tile, 2, 44, 57));
        this.addSlot(new SlotTakeOnly(tile, 3, 116, 21));
        this.addSlot(new SlotTakeOnly(tile, 4, 134, 21));
        this.addSlot(new SlotTakeOnly(tile, 5, 116, 39));
        this.addSlot(new SlotTakeOnly(tile, 6, 134, 39));
        this.addSlot(new SlotTakeOnly(tile, 7, 116, 57));
        this.addSlot(new SlotTakeOnly(tile, 8, 134, 57));
        this.addSlot(new SlotNonRetarded(tile, 9, 8, 21));

        playerInv(playerInv, 8, 116);
    }
}
