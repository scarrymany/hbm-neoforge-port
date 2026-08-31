package com.hbm.inventory.container.machine.accel;

import com.hbm.blockentity.machine.accel.PaDetectorBlockEntity;
import com.hbm.inventory.container.MenuBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code ContainerPADetector}: battery + 2 in + 2 out. */
public class PaDetectorMenu extends MenuBase<PaDetectorBlockEntity> {

    public PaDetectorMenu(int id, Inventory playerInv, PaDetectorBlockEntity be) {
        super(AccelMenus.PA_DETECTOR.get(), id, be);
        this.addSlot(new SlotNonRetarded(tile, 0, 8, 21));
        this.addSlot(new SlotNonRetarded(tile, 1, 62, 39));
        this.addSlot(new SlotNonRetarded(tile, 2, 80, 39));
        this.addSlot(new SlotTakeOnly(tile, 3, 116, 39));
        this.addSlot(new SlotTakeOnly(tile, 4, 134, 39));
        playerInv(playerInv, 8, 104);
    }
}
